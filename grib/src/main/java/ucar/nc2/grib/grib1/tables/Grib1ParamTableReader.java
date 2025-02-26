/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1.tables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import thredds.client.catalog.Catalog;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.grib1.*;
import ucar.unidata.util.StringUtil2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Grib1 Parameter Table (table 2). This is a map: code -> Grib1Parameter. Handles reading the
 * table in from various formats
 *
 * @author caron
 * @since 11/16/11
 */
public class Grib1ParamTableReader {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1ParamTableReader.class);
  private static final boolean debug = false;

  private int center_id;
  private int subcenter_id;
  private int version;
  private final String name;  // name of the table
  private String path; // path of filename containing this table
  private String desc; // optional desc from within the file
  private Map<Integer, Grib1Parameter> parameters; // param number -> param
  private boolean useName;

  /**
   * Read a dataset-specific table from a file
   *
   * @param path read from this path, may be reletive
   */
  public Grib1ParamTableReader(String path) {
    this.path = StringUtil2.replace(path, "\\", "/");
    File f = new File(path);
    this.name = f.getName();
    this.parameters = readParameterTable();
  }

  /**
   * Create a standard table from a file. Defer reading until getParameters is called.
   *
   * @param center_id associate with this center
   * @param subcenter_id associate with this subcenter
   * @param version associate with this version
   * @param path read from this path, may be reletive
   */
  Grib1ParamTableReader(int center_id, int subcenter_id, int version, String path) {
    this.center_id = center_id;
    this.subcenter_id = subcenter_id;
    this.version = version;
    this.path = StringUtil2.replace(path, "\\", "/");
    File f = new File(path);
    this.name = f.getName();
  }

  /**
   * Create a dataset-specific table from a jdom tree using the DSS parser
   *
   * @param paramTableElem the jdom tree
   */
  Grib1ParamTableReader(org.jdom2.Element paramTableElem) {
    this.name = paramTableElem.getChildText("title");
    DssParser p = new DssParser(Catalog.ncmlNS);
    this.parameters = p.parseXml(paramTableElem);
  }

  public int getCenter_id() {
    return center_id;
  }

  public int getSubcenter_id() {
    return subcenter_id;
  }

  public int getVersion() {
    return version;
  }

  public String getName() {
    return name;
  }

  public String getDesc() {
    return desc;
  }

  // whether to use the param name or description in the variable name
  public boolean useParamName() {
    return useName;
  }

  public int getKey() {
    return Grib1ParamTables.makeKey(center_id, subcenter_id, version);
  }

  public String getPath() {
    return path;
  }

  public Map<Integer, Grib1Parameter> getParameters() {
    if (parameters == null) {
      parameters = readParameterTable();
    }
    return parameters;
  }

  public void setParameters(Map<Integer, Grib1Parameter> params) {
    parameters = params;
  }

  /**
   * Get the parameter with id. If not found, look in default table.
   *
   * @param id the parameter number
   * @return the Grib1Parameter, or null if not found
   */
  public Grib1Parameter getParameter(int id) {
    if (parameters == null) {
      parameters = readParameterTable();
    }

    return parameters.get(id);
  }

  /**
   * Get the parameter with id, but dont look in default table.
   *
   * @param id the parameter number
   * @return the Grib1Parameter, or null if not found
   */
  public Grib1Parameter getLocalParameter(int id) {
    if (parameters == null) {
      parameters = readParameterTable();
    }
    return parameters.get(id);
  }

  @Override
  public String toString() {
    return "Grib1ParamTable{" +
        "center_id=" + center_id +
        ", subcenter_id=" + subcenter_id +
        ", version=" + version +
        ", name='" + name + '\'' +
        ", path='" + path + '\'' +
        '}';
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // reading

  private synchronized Map<Integer, Grib1Parameter> readParameterTable() {
    if (path == null) {
      throw new IllegalStateException(name);
    }

    try {
      if (name.startsWith("table_2_") || name.startsWith("local_table_2_")) {
        return readParameterTableEcmwf(); // ecmwf
      } else if (name.startsWith("US058")) {
        return readParameterTableXml(new FnmocParser());// FNMOC
      } else if (name.endsWith(".tab")) {
        return readParameterTableTab();                               // wgrib format
      } else if (name.endsWith(".wrf")) {
        return readParameterTableSplit("\\|", new int[]{0, 3, 1, 2}); // WRF AMPS
      } else if (name.endsWith(".h")) {
        return readParameterTableNcl(); // NCL
      } else if (name.endsWith(".dss")) {
        return readParameterTableSplit("\t", new int[]{0, -1, 1, 2}); // NCAR DSS
      } else if (name.endsWith(".xml")) {
        return readParameterTableXml(new DssParser(Namespace.NO_NAMESPACE));// NCAR DSS XML format
      } else if (name.startsWith("2.98")) {
        return readParameterTableEcmwfEcCodes(); // ecmwf from ecCodes package
      } else {
        throw new RuntimeException(
            "Grib1ParamTableReader: Dont know how to read " + name + " file=" + path);
      }

    } catch (IOException ioError) {
      logger.warn(
          "An error occurred in Grib1ParamTable while trying to open the parameter table {}:{}",
          path, ioError.getMessage());
      throw new RuntimeException(ioError);
    }
  }

/*
 * Brazilian Space Agency - INPE/CPTEC
 * Center: 46
 * Subcenter: 0
 * Parameter table version: 254
 *

TBLE2 cptec_254_params[] = {
{1, "Pressure", "hPa", "PRES"},
{2, "Pressure reduced to MSL", "hPa", "PSNM"},
{3, "Pressure tendency", "Pa/s", "TSPS"},
{6, "Geopotential", "dam", "GEOP"},
{7, "Geopotential height", "gpm", "ZGEO"},
{8, "Geometric height", "m", "GZGE"},
{11, "ABSOLUTE TEMPERATURE", "K", "TEMP"},

   */

  private static final Pattern nclPattern = Pattern.compile("\\{(\\d*),\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\".*");

  private Map<Integer, Grib1Parameter> readParameterTableNcl() throws IOException {
    HashMap<Integer, Grib1Parameter> result = new HashMap<>();

    try (InputStream is = GribResourceReader.getInputStream(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

      // Ignore header
      while (true) {
        String line = br.readLine();
        if (line == null) {
          break; // done with the file
        }
        if (line.startsWith("TBLE2")) {
          break;
        }

        if (line.contains("Center:")) {
          center_id = extract(line, "Center:");
        } else if (line.contains("Subcenter:")) {
          subcenter_id = extract(line, "Subcenter:");
        } else if (line.contains("version:")) {
          version = extract(line, "version:");
        }
      }

      while (true) {
        String line = br.readLine();
        if (line == null) {
          break; // done with the file
        }
        if ((line.length() == 0) || line.startsWith("#")) {
          continue;
        }

        Matcher m = nclPattern.matcher(line);
        if (!m.matches()) {
          continue;
        }

        int p1;
        try {
          p1 = Integer.parseInt(m.group(1));
        } catch (Exception e) {
          logger.warn("Cant parse " + m.group(1) + " in file " + path);
          continue;
        }
        Grib1Parameter parameter = new Grib1Parameter(this, p1, m.group(4), m.group(2),
            m.group(3));
        result.put(parameter.getNumber(), parameter);
        logger.debug(" %s%n", parameter);
      }

      return Collections.unmodifiableMap(result);  // all at once - thread safe
    }

  }

  private int extract(String line, String key) {
    int pos = line.indexOf(key);
    if (pos < 0) {
      return -1;
    }
    String want = line.substring(pos + key.length());
    try {
      return Integer.parseInt(want.trim());
    } catch (NumberFormatException e) {
      logger.warn("BAD %s (%s)%n", line, path);
      return -1;
    }
  }

  /*
  WMO standard table 2: Version Number 3.
  Codes and data units for FM 92-X Ext.GRIB.
  ......................
  001
  P
  Pressure
  Pa
  Pa
  ......................
  002
  MSL
  Mean sea level pressure
  Pa
  Pa
  ......................
  003
  None
  Pressure tendency
  Pa s**-1
  Pa s**-1
  ......................
  004
  PV
  Potential vorticity
  K m**2 kg**-1 s**-1
  K m**2 kg**-1 s**-1
  ......................
  005
  None
  ICAO Standard Atmosphere reference height
  m
  m
   */

  private Map<Integer, Grib1Parameter> readParameterTableEcmwf() throws IOException {
    HashMap<Integer, Grib1Parameter> result = new HashMap<>();

    try (InputStream is = GribResourceReader.getInputStream(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      String line = br.readLine();
      if (line == null) {
        throw new FileNotFoundException(path + " is empty");
      }
      if (!line.startsWith("...")) {
        this.desc = line; // maybe ??
      }

      while (line != null && !line.startsWith("...")) {
        line = br.readLine(); // skip
      }

      while (true) {
        line = br.readLine();
        if (line == null) {
          break; // done with the file
        }
        if ((line.length() == 0) || line.startsWith("#")) {
          continue;
        }
        if (line.startsWith("...")) { // ...  may have already been read
          line = br.readLine();
          if (line == null) {
            break;
          }
        }
        String num = line.trim();
        line = br.readLine();
        String name = (line != null) ? line.trim() : null;
        line = br.readLine();
        String desc = (line != null) ? line.trim() : null;
        line = br.readLine();
        String units1 = (line != null) ? line.trim() : null;

        // optional notes
        line = br.readLine();
        String notes = (line == null || line.startsWith("...")) ? null : line.trim();
        if (desc != null && desc.equalsIgnoreCase("undefined")) {
          continue; // skip
        }

        int p1;
        try {
          p1 = Integer.parseInt(num);
        } catch (Exception e) {
          logger.warn("Cant parse " + num + " in file " + path);
          continue;
        }
        Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, units1);
        result.put(parameter.getNumber(), parameter);
        logger.debug(" %s (%s)%n", parameter, notes);
      }
      return Collections.unmodifiableMap(result);  // all at once - thread safe
    }

  }

  /**
   * This method will read in ECMWF grib1 tables. Note that these tables are generated locally by
   * Unidata, and come directly from the ECMWF GRIB-API package localConcepts files. They are
   * generated by:
   * <p>
   * ucar.nc2.grib.grib1.tables.EcmwfLocalConcepts
   * <p>
   * The original localConcepts files are located in:
   * <p/>
   * grib/src/main/sources/ecmwfEcCodes/
   * <p/>
   * Since we write the table file that are ultimately read by CDM, the format is controled and is
   * the following:
   * <p>
   * paramNum shortName [description] (units)
   * <p>
   * for example,
   * <p>
   * 251 atte [Adiabatic tendency of temperature] (K)
   */
  private Map<Integer, Grib1Parameter> readParameterTableEcmwfEcCodes() throws IOException {
    HashMap<Integer, Grib1Parameter> result = new HashMap<>();

    try (InputStream is = GribResourceReader.getInputStream(path);
      BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      String line = br.readLine();
      if (line == null) {
        throw new FileNotFoundException(path + " is empty");
      }

      // create table name from file name
      String[] splitPath = path.split("/");
      String tableNum = splitPath[splitPath.length - 1].replace(".table", "");
      this.desc = "ECMWF GRIB API TABLE " + tableNum;

      // skip header
      while (line != null && !line.startsWith("#")) {
        line = br.readLine(); // skip
      }

      // keep going until the end of the file is reached (line == null)
      while (true) {
        // exmaple: 251 atte [Adiabatic tendency of temperature] (K)
        line = br.readLine();
        if (line == null) {
          break; // done with the file
        }
        if ((line.length() == 0) || line.startsWith("#")) {
          continue;
        }

        // get unit - (K)
        String[] tmpUnitArray = line.split("\\(");
        String tmpUnit = tmpUnitArray[tmpUnitArray.length - 1];
        int lastUnitIndex;
        while ((lastUnitIndex = tmpUnit.lastIndexOf(")")) > 0) {
          tmpUnit = tmpUnit.substring(0, lastUnitIndex).trim();
        }
        String unit = tmpUnit.trim();
        // unit = Util.cleanUnit(unit); // fixes some common unit mistakes  // jcaron - just use unit as it is

        // get parameter number - 251
        String[] lineArray = line.trim().split("\\s+"); // all and any white space
        String num = lineArray[0];

        // get shortName - atte
        String name = lineArray[1].trim();
        //if (name.equals("~")) {}; - todo create name from long name(?)

        // get description. bracketed by [] - [Adiabatic Tendency of temperature]
        int startDesc = line.indexOf("[");
        int endDesc = line.indexOf("]");
        String desc = line.substring(startDesc, endDesc).trim();

        // stuff information into a Grib1Parameter object
        int p1;
        try {
          p1 = Integer.parseInt(num);
        } catch (Exception e) {
          logger.warn("Cant parse " + num + " in file " + path);
          continue;
        }
        Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, unit);
        result.put(parameter.getNumber(), parameter);
        logger.debug(" %s%n", parameter);
      }

      return Collections.unmodifiableMap(result);  // all at once - thread safe
    }
  }

  private Map<Integer, Grib1Parameter> readParameterTableXml(XmlTableParser parser) throws IOException {
    try (InputStream is = GribResourceReader.getInputStream(path)) {
      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();
      return parser.parseXml(root);  // all at once - thread safe

    } catch (JDOMException | IOException e) {
      throw new IOException(e);
    }
  }

  private interface XmlTableParser {
    Map<Integer, Grib1Parameter> parseXml(Element root);
  }

  private class DssParser implements XmlTableParser {

    private final Namespace ns;

    DssParser(Namespace ns) {
      this.ns = ns;
    }

    /* http://dss.ucar.edu/metadata/ParameterTables/WMO_GRIB1.60-1.3.xml
     <parameter code="5">
     <description>ICAO Standard Atmosphere reference height</description>
     <units>m</units>
     </parameter>
     */
    public Map<Integer, Grib1Parameter> parseXml(Element root) {
      Map<Integer, Grib1Parameter> result = new HashMap<>();
      List<Element> params = root.getChildren("parameter", ns);
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description", ns);
        if (desc == null) {
          continue;
        }
        String units = elem1.getChildText("units", ns);
        if (units == null) {
          units = "";
        }
        String name = elem1.getChildText("name", ns);
        if (name == null) {
          name = elem1.getChildText("shortName", ns);
        }
        String cf = elem1.getChildText("CF", ns);
        Grib1Parameter parameter = new Grib1Parameter(Grib1ParamTableReader.this, code, name, desc,
            units, cf);
        result.put(parameter.getNumber(), parameter);
        logger.debug(" %s%n", parameter);
      }
      return Collections.unmodifiableMap(result);  // all at once - thread safe
    }
  }

  private class FnmocParser implements XmlTableParser {
    /*
    <pnTable>
      <name>Master Parameter Table</name>
      <updatedDTG>201110110856</updatedDTG>
      <tableStatus setDTG="2007050712">current</tableStatus>
      <fnmocTable>
        <entry>
          <grib1Id>001</grib1Id>
          <fnmocId>pres</fnmocId>
          <name>pres</name>
          <nameFull/>
          <description>Commonly used for atmospheric pressure, the pressure exerted by the atmosphere as a consequence of
            gravitational attraction exerted upon the "column" of air lying directly above the point in question.
          </description>
          <unitsFNMOC>pa</unitsFNMOC>
          <productionStatus>current</productionStatus>
        </entry>
    */

    public HashMap<Integer, Grib1Parameter> parseXml(Element root) {
      HashMap<Integer, Grib1Parameter> result = new HashMap<>();
      Element fnmocTable = root.getChild("fnmocTable");
      List<Element> params = fnmocTable.getChildren("entry");
      for (Element elem1 : params) {
        int code;
        try {
          code = Integer.parseInt(elem1.getChildText("grib1Id"));
        } catch (NumberFormatException e) {
          logger.warn("BAD number= %s%n", elem1.getChildText("grib1Id"));
          continue;
        }
        String desc = elem1.getChildText("description");
        if (desc == null) {
          continue;
        }
        //if (desc.startsWith("no definition")) continue; // skip; use standard def
        desc = StringUtil2.collapseWhitespace(desc);
        String units = elem1.getChildText("unitsFNMOC");
        if (units == null) {
          units = "";
        }
        String name = elem1.getChildText("name");
        Grib1Parameter parameter = new Grib1Parameter(Grib1ParamTableReader.this, code, name, desc,
            units, null);
        result.put(parameter.getNumber(), parameter);
        logger.debug(" %s%n", parameter);
      }
      useName = true;
      return result;
    }
  }

  // order: num, name, desc, unit
  private Map<Integer, Grib1Parameter> readParameterTableSplit(String regexp, int[] order)
      throws IOException {
    HashMap<Integer, Grib1Parameter> result = new HashMap<>();

    try (InputStream is = GribResourceReader.getInputStream(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

      // rdg - added the 0 line length check to cover the case of blank lines at the end of the parameter table file.
      while (true) {
        String line = br.readLine();
        if (line == null) {
          break;
        }
        if ((line.length() == 0) || line.startsWith("#")) {
          continue;
        }
        String[] flds = line.split(regexp);

        int p1 = Integer.parseInt(flds[order[0]].trim()); // must have a number
        String name = (order[1] >= 0) ? flds[order[1]].trim() : null;
        String desc = flds[order[2]].trim();
        String units = (flds.length > order[3]) ? flds[order[3]].trim() : "";

        Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, units);
        result.put(parameter.getNumber(), parameter);
        logger.debug(" %s%n", parameter);
      }
      return Collections.unmodifiableMap(result);  // all at once - thread safe
    }

  }

  private Map<Integer, Grib1Parameter> readParameterTableTab() throws IOException {

    try (InputStream is = GribResourceReader.getInputStream(path)) {
      HashMap<Integer, Grib1Parameter> params;
      try (BufferedReader br = new BufferedReader(
          new InputStreamReader(is, StandardCharsets.UTF_8))) {
        br.readLine(); // skip a line

        // thread safe - local var
        params = new HashMap<>();
        while (true) {
          String line = br.readLine();
          if (line == null) {
            break;
          }
          if ((line.length() == 0) || line.startsWith("#")) {
            continue;
          }
          String[] tableDefArr = line.split(":");

          int p1 = Integer.parseInt(tableDefArr[0].trim());
          String name = tableDefArr[1].trim();
          String desc, units;
          // check to see if unit defined, if not, parameter is undefined
          if (tableDefArr[2].indexOf('[') == -1) {
            // Undefined unit
            desc = tableDefArr[2].trim();
            units = "";
          } else {
            String[] arr2 = tableDefArr[2].split("\\[");
            desc = arr2[0].trim();
            units = arr2[1].substring(0, arr2[1].lastIndexOf(']')).trim();
          }

          Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, units);
          if (!parameter.getDescription().equalsIgnoreCase("undefined")) {
            params.put(parameter.getNumber(), parameter);
          }
          logger.debug(parameter.getNumber() + " " + parameter.getDescription() + " " + parameter.getUnit());
        }
      }
      return Collections.unmodifiableMap(params);  // all at once - thread safe
    }

  }
}
