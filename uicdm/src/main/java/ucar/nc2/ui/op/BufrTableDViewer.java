/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import ucar.nc2.iosp.bufr.tables.BufrTables;
import ucar.nc2.iosp.bufr.tables.TableD;
import ucar.ma2.DataType;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.io.*;
import javax.swing.*;

/**
 * View BUFR Table D
 *
 * @author caron
 * @since Dec 1, 2009
 */
public class BufrTableDViewer extends JPanel {
  private PreferencesExt prefs;

  private BeanTable ddsTable, variantTable;
  private JSplitPane split;

  private TextHistoryPane compareTA;
  private IndependentWindow infoWindow;

  private TableD currTable;

  private HashMap<Short, List<String>> usedDds = null;

  public BufrTableDViewer(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    ddsTable = new BeanTable(DdsBean.class, (PreferencesExt) prefs.node("DdsBean"), false);
    ddsTable.addListSelectionListener(e -> {
        DdsBean csb = (DdsBean) ddsTable.getSelectedBean();
        showVariants(csb);
    });

    variantTable = new BeanTable(DdsBean.class, (PreferencesExt) prefs.node("VariantBean"), false);
    /* variantTable.addListSelectionListener(e -> {
        variantTable.getSelectedBean();
    }); */

    PopupMenu varPopup = new PopupMenu(ddsTable.getJTable(), "Options");
    varPopup.addAction("Show uses", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        DdsBean csb = (DdsBean) ddsTable.getSelectedBean();
        if (usedDds != null) {
          List<String> list = usedDds.get(csb.dds.getId());
          if (list != null) {
            for (String use : list)
              out.format(" %s%n", use);
          }
        }
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    });

    varPopup = new PopupMenu(variantTable.getJTable(), "Options");
    varPopup.addAction("Show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        DdsBean ddsBean = (DdsBean) variantTable.getSelectedBean();
        if (ddsBean != null) {
          ddsBean.dds.show(out, false);
        }
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    });

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Compare to standard table", false);
    compareButton.addActionListener(e -> {
        try {
          Formatter out = new Formatter();
          TableD wmoTable = BufrTables.getWmoTableD(13);
          if (wmoTable != null)
            compare(currTable, wmoTable, out);

          compareTA.setText(out.toString());
          compareTA.gotoTop();
          infoWindow.setVisible(true);

        } catch (Throwable ioe) {
          StringWriter sw = new StringWriter(10000);
          ioe.printStackTrace(new PrintWriter(sw));
          compareTA.setText(sw.toString());
          compareTA.gotoTop();
          infoWindow.setVisible(true);
          ioe.printStackTrace();
        }
    });
    buttPanel.add(compareButton);

    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    /* the info window 2
    infoTA2 = new TextHistoryPane();
    infoWindow2 = new IndependentWindow("Extra Information-2", BAMutil.getImage("nj22/NetcdfUI"), infoTA2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ddsTable, obsTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800)); */

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ddsTable, variantTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public void save() {
    ddsTable.saveState(false);
    variantTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
    //if (fileChooser != null) fileChooser.save();
  }

  public void setBufrTableD(String filename, BufrTables.Format format) throws IOException {
    TableD tableD = BufrTables.readTableD(filename, format, true);
    int pos = filename.lastIndexOf("/");
    String src = (pos > 0) ? filename.substring(pos + 1) : filename;

    List<TableD.Descriptor> listDesc = new ArrayList<>(tableD.getDescriptors());
    Collections.sort(listDesc);
    List<DdsBean> dds = new ArrayList<>(listDesc.size());
    for (TableD.Descriptor d : listDesc) {
      dds.add(new DdsBean(src, d));
    }
    ddsTable.setBeans(dds);
    currTable = tableD;
  }

  private void compare(TableD t1, TableD t2, Formatter out) {
    List currBeans = ddsTable.getBeans();

    out.format("Compare Table D%n %s %n %s %n", t1.getName(), t2.getName());
    boolean err = false;
    List<TableD.Descriptor> listDesc = new ArrayList<>(t1.getDescriptors());
    Collections.sort(listDesc);
    for (Object bean : currBeans) {
      DdsBean dbean = (DdsBean) bean;
      dbean.diff = null;

      TableD.Descriptor d1 = dbean.dds;
      TableD.Descriptor d2 = t2.getDescriptor(d1.getId());
      if (d2 == null) {
        err = true;
        out.format(" **No key %s in second table %n", d1.getFxy());
        dbean.diff = "new";
      } else {
        List<Short> seq1 = d1.getSequence();
        List<Short> seq2 = d2.getSequence();
        if (seq1.size() != seq2.size()) {
          err = true;
          out.format(" **key %s size %d != %d %n  ", d1.getFxy(), seq1.size(), seq2.size());
          for (Short f1 : seq1) out.format(" %s,", fxy(f1));
          out.format("%n  ");
          for (Short f2 : seq2) out.format(" %s,", fxy(f2));
          out.format("%n");
          dbean.diff = "seq";
        } else {
          for (int i = 0; i < seq1.size(); i++) {
            short fxy1 = seq1.get(i);
            short fxy2 = seq2.get(i);
            if (fxy1 != fxy2) {
              err = true;
              out.format(" **MISMATCH key %s feature %s != %s %n", d1.getFxy(), fxy(fxy1), fxy(fxy2));
              for (Short f1 : seq1) out.format(" %s,", fxy(f1));
              out.format("%n");
              for (Short f2 : seq2) out.format(" %s,", fxy(f2));
              out.format("%n");
              dbean.diff = "seq";
            }
          }
        }
      }
    }
    if (!err) out.format("All OK%n");

    // see whats missing
    for (TableD.Descriptor d2 : t2.getDescriptors()) {
      TableD.Descriptor d1 = t1.getDescriptor(d2.getId());
      if (d1 == null) {
        out.format(" **No key %s in first table %n", d2.getFxy());
      }
    }

  }

  String fxy(short id) {
    return ucar.nc2.iosp.bufr.Descriptor.makeString(id);
  }

  /* private HashMap<Short, List<DdsBean>> allVariants = null;

  private void loadVariants() {
    allVariants = new HashMap<Short, List<DdsBean>>();
    try {
      loadVariant("wmo-v07", BufrTables.getWmoTableD(7));
      loadVariant("wmo-v08", BufrTables.getWmoTableD(8));
      loadVariant("wmo-v09", BufrTables.getWmoTableD(9));
      loadVariant("wmo-v10", BufrTables.getWmoTableD(10));
      loadVariant("wmo-v11", BufrTables.getWmoTableD(11));
      loadVariant("wmo-v13", BufrTables.getWmoTableD(12));
      loadVariant("wmo-v12", BufrTables.getWmoTableD(13));
      loadVariant("wmo-v14", BufrTables.getWmoTableD(14));
      loadVariant("wmo-v15", BufrTables.getWmoTableD(15));
      loadVariant("wmo-v16", BufrTables.getWmoTableD(16));
      /* loadVariant("ours-v13", BufrTables.readTableD("C:/dev/tds4.1/thredds/bufrTables/src/main/sources/archive/B4M-000-013-D", BufrTables.Format.mel_bufr, false));
      loadVariant("ncep-v13", BufrTables.readTableD("C:/dev/tds4.1/thredds/bufrTables/src/main/sources/ncep/bufrtab.TableD_STD_0_13", BufrTables.Format.ncep, false));
      loadVariant("ncep-v14", BufrTables.readTableD("C:/dev/tds4.1/thredds/bufrTables/src/main/sources/ncep/bufrtab.TableD_STD_0_14", BufrTables.Format.ncep, false));
      loadVariant("ecmwf-v13", BufrTables.readTableD("C:/dev/tds4.1/thredds/bufrTables/src/main/sources/ecmwf/D0000000000098013001.TXT", BufrTables.Format.ecmwf, false)); */
      /* loadVariant("bmet-v13", BufrTables.readTableD("C:/dev/tds/thredds/bufrTables/src/main/sources/bmet/BUFR_B_080731.xml", "bmet"));
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  private void loadVariant(String src, TableD table) {
    List<TableD.Descriptor> listDesc = new ArrayList<TableD.Descriptor>(table.getDescriptors());
    for (TableD.Descriptor d : listDesc) {
      List<DdsBean> list = allVariants.get(d.getId());
      if (list == null) {
        list = new ArrayList<DdsBean>(10);
        allVariants.put(d.getId(), list);
      }
      list.add(new DdsBean(src, d));
    }
  } */

  //////////////////////////////////////////////////////
  private HashMap<Short, List<DdsBean>> allVariants = null;
  private Set<String> variantKeys = null;
  private boolean standardVariantsLoaded = false;
  private void loadStandardVariants() {
    for (BufrTables.TableConfig tc : BufrTables.getTables()) {
      try {
        if (tc.getTableDname() != null)
          loadVariant(tc.getName(), BufrTables.readTableD(tc.getTableDname(), tc.getTableDformat(), false));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    standardVariantsLoaded = true;
  }

  private void loadVariant(String key, TableD table) {
    if (table == null) {
      System.out.printf("File to open TableD = %s%n", key);
      return;
    }

    if (allVariants == null) allVariants = new HashMap<>();
    if (variantKeys == null) variantKeys = new HashSet<>();
    if (variantKeys.contains(key)) return; // dont add again

    List<TableD.Descriptor> listDesc = new ArrayList<>(table.getDescriptors());
    for (TableD.Descriptor d : listDesc) {
      List<DdsBean> list = allVariants.computeIfAbsent(d.getId(), k -> new ArrayList<>(10));
      list.add(new DdsBean(key, d));
    }
  }

  private void showVariants(DdsBean bean) {
    if (!standardVariantsLoaded) loadStandardVariants();
    List<DdsBean> all = allVariants.get(bean.dds.getId());
    List<DdsBean> dds = new ArrayList<>(10);
    dds.add(bean);
    if (all != null) dds.addAll(all);
    variantTable.setBeans(dds);
  }

  /////////////////////////////////////////////////////////

  public class DdsBean {
    TableD.Descriptor dds;
    String source;
    String diff;

    // create from a dataset
    public DdsBean(String source, TableD.Descriptor dds) {
      this.source = source;
      this.dds = dds;
    }

    public String getFxy() {
      return dds.getFxy();
    }

    public int getId() {
      return DataType.unsignedShortToInt(dds.getId());
    }

    public String getSource() {
      return source;
    }

    public String getName() {
      return dds.getName();
    }

    public String getSequence() {
      Formatter out = new Formatter();
      for (short s : dds.getSequence())
        out.format(" %s,", ucar.nc2.iosp.bufr.Descriptor.makeString(s));
      return out.toString();
    }

    public int getUsed() {
      if (usedDds == null) return 0;
      List<String> list = usedDds.get(dds.getId());
      if (list == null) return 0;
      return list.size();
    }

    public boolean isLocal() {
      return dds.isLocal();
    }

    public String getDiff() {
      return diff;
    }
  }
}
