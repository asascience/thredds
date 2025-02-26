/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1.tables;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The purpose of this class is to read in the files from ECMWFs ecCodes software and
 * create useful grib 1 tables for the CDM. Note that the intent is to process these
 * local concept files as minimally as possible. Only run this from the git directory.
 * This is run offline
 */
public class EcmwfLocalConcepts {
    // super hash map keys
    private static final String SHORTNAME_ID = "shortName";
    private static final String DESCRIPTION_ID = "description";
    private static final String UNIT_ID = "units";

    // IDs used in localConcept files
    private static final String TABLE_VERSION_ID = "table2Version";
    private static final String PARAM_NUM_ID = "indicatorOfParameter";

    private static final Charset ENCODING = StandardCharsets.UTF_8;

    // tableNumber -> paramNumber -> metadata from table
    private final HashMap<String, HashMap<String, HashMap<String,String>>> localConcepts = new HashMap<>();

    // location of the localConcept files
    private final String ecmwfLocalConceptsLoc;

    // default constructor
    public EcmwfLocalConcepts() {
        // find path to localConcept files

        String sep = File.separator;
        String classPath = EcmwfLocalConcepts.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String split = "thredds"+sep+"grib";
        String sourcesPath = classPath.split(split)[0];
        if (classPath.equals(sourcesPath)) {
            split = "grib"+sep+"build";
            sourcesPath = classPath.split(split)[0];
            sourcesPath = classPath.split("/grib")[0];
        }
        ecmwfLocalConceptsLoc = sourcesPath+sep+"grib"+sep+"src"+sep+"main"+sep+"sources"+sep+"ecmwfEcCodes"+sep;
        // initialize input streams for reading the localConcept files
        try {
          parseLocalConcept(ecmwfLocalConceptsLoc + "shortName.def", SHORTNAME_ID);
          parseLocalConcept(ecmwfLocalConceptsLoc + "name.def", DESCRIPTION_ID);
          parseLocalConcept(ecmwfLocalConceptsLoc + "units.def", UNIT_ID);
        } catch  (IOException e) {
            e.printStackTrace();
        }
    }

    /** Parse the localConcept files needed to create grib1 tables for use by the CDM. */
    private void parseLocalConcept(String filename, String conceptName) throws IOException {
      try (InputStream is = new FileInputStream(filename)) {
        addLocalConcept(is, conceptName);
      }
    }

    /**
     * Add the information from a localConcept file to super HashMap localConcepts
     *
     * @param is InputStream of the localConcept file
     * @param conceptName "type" of localConcept being added
     */
    private void addLocalConcept(InputStream is, String conceptName) throws IOException {
        /*
        example entry from name.def:

        #Total precipitation of at least 5 mm
        'Total precipitation of at least 5 mm' = {
             table2Version = 131 ;
             indicatorOfParameter = 61 ;
            }
         */

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, ENCODING))) {
            String line = br.readLine();

            while (!line.startsWith("#"))
                line = br.readLine(); // skip

            while (true) {
                HashMap<String, String> items = new HashMap<>();
                line = br.readLine();
                if (line == null) break; // done with the file
                if ((line.length() == 0) || line.startsWith("#")) continue;
                line = cleanLine(line);
                if (line.contains("{")) {
                    String paramName = line.split("=")[0].trim();
                    line = br.readLine();
                    if (line == null) break; // done with the file
                    line = cleanLine(line);
                    while (line.contains("=")) {
                        String[] kvp = line.split("=");
                        items.put(kvp[0].trim(), kvp[1].trim());
                        line = br.readLine();
                        if (line == null) break; // done with the file
                        line = cleanLine(line);                    }
                    String tableVersion = items.get(TABLE_VERSION_ID);
                    String parameterNumber = items.get(PARAM_NUM_ID);

                    storeConcept(tableVersion, parameterNumber, conceptName, paramName);
                }
            }
        }
    }

    /**
     * clean the string representation of a line in the localConcept
     * file. Basic removal of tabs, semicolons, single quotes, etc.
     * @param lineIn  line (String) to be cleaned
     * @return cleaned version of lineIn
     */
    private String cleanLine(String lineIn) {
        String lineOut;
        lineOut = lineIn.replaceAll("'", "");
        lineOut = lineOut.replaceAll("\t", "");
        lineOut = lineOut.replaceAll(";", "");

        return lineOut.trim();
    }

    /**
     * Store localConcept information in super HashMap localConcepts
     *
     * @param tableVersion  - version of the table to add parameter to
     * @param parameterNumber  - parameter number
     * @param key Type of metadata to be added (shortName, Description, Units)
     * @param value Value of metadata
     */
    private void storeConcept(String tableVersion, String parameterNumber, String key, String value) {

        HashMap<String, HashMap<String, String>> tmpTable;
        if (localConcepts.containsKey(tableVersion)) {
            tmpTable = localConcepts.get(tableVersion);
            if (tmpTable.containsKey(parameterNumber)) {
              HashMap<String, String> tmpParam = tmpTable.get(parameterNumber);
                if (!tmpParam.containsKey(key)) {
                     tmpParam.put(key, value);
                } else {
                    System.out.println("already has key value pair: " + key + ":" + value);
                }
            } else {
                HashMap<String, String> tmpParam = new HashMap<>(4);
                tmpParam.put(key, value);
                tmpTable.put(parameterNumber, tmpParam);
            }
        } else {
            tmpTable = new HashMap<>();
            HashMap<String, String> tmpParam = new HashMap<>(4);
            tmpParam.put(key, value);
            tmpTable.put(parameterNumber, tmpParam);
        }
        localConcepts.put(tableVersion, tmpTable);
    }

    /**
     * Write out grib1 tables based on localConcepts files - these are the tables
     * that the CDM will read.
     */
    private void writeGrib1Tables() throws IOException {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
        Calendar cal = Calendar.getInstance();
        String writeDate = dateFormat.format(cal.getTime());
        String grib1Info;
        List<String> tableNums = new ArrayList<>();
        HashMap<String, String> paramInfo;
        Path dir = Paths.get(ecmwfLocalConceptsLoc.replace("sources/", "resources/resources/grib1/"));
        for (String tableNum : localConcepts.keySet()) {
            tableNums.add(tableNum);
            String fileName = "2.98." + tableNum + ".table";
            System.out.println("Writing: " + fileName);
            Path newFile = dir.resolve(fileName);
            Files.deleteIfExists(newFile);
            Files.createFile(newFile);

            try (BufferedWriter writer = Files.newBufferedWriter(newFile, ENCODING)){
                writer.write("# Generated by " + this.getClass().getCanonicalName() + " on " + writeDate);
                writer.newLine();
                for(String paramNum : localConcepts.get(tableNum).keySet()){
                    paramInfo = localConcepts.get(tableNum).get(paramNum);

                    String shortName = paramInfo.get(SHORTNAME_ID);
                    String description = paramInfo.get(DESCRIPTION_ID);
                    String units = paramInfo.get(UNIT_ID);

                    grib1Info = paramNum + " " + shortName + " [" + description + "] (" + units + ")";

                    writer.write(grib1Info);
                    writer.newLine();
                }
            }
        }
        writeLookupTableFile(tableNums, dir, writeDate);
    }

    /**
     * Write the lookupTables.txt file, which basically registers all of the new grib1 tables
     * with the CDM
     *
     * @param tableNums List of Table Numbers
     * @param dir Directory where the tables live
     * @param writeDate Date on which the main method of this class was run, resulting in new tables
     */
    private void writeLookupTableFile(List<String> tableNums, Path dir, String writeDate) throws IOException {

        System.out.println("Writing: lookupTables.txt");
        Collections.sort(tableNums);
        Path lookupTableReg = dir.resolve("lookupTables.txt");
        Files.deleteIfExists(lookupTableReg);
        Files.createFile(lookupTableReg);
        try (BufferedWriter writer = Files.newBufferedWriter(lookupTableReg, ENCODING)){
            writer.write("# Generated by " + this.getClass().getCanonicalName() + " on " + writeDate);
            writer.newLine();
            for(String tn : tableNums){
                String tableName = "2.98." + tn + ".table";
                String reg = "98:\t-1:\t" + tn + ":\t" + tableName;
                writer.write(reg);
                writer.newLine();
            }
        }
    }

    /**
     * Quick prinout to System.out of the different parameter metadata fields
     */
    private void showLocalConcepts() {
        for (String tableNum : localConcepts.keySet()) {
            for (String paramNum : localConcepts.get(tableNum).keySet()) {
                for (String key :localConcepts.get(tableNum).get(paramNum).keySet()) {
                   System.out.println(key + ":" + localConcepts.get(tableNum).get(paramNum).get(key));
                }
            }
        }
    }

    /**
     * Generate grib1 tables for the CDM based on the localConcept files from ECMWF GRIB-API
     *
     * @param args None
     */
    public static void main(String[] args) {

        EcmwfLocalConcepts ec = new EcmwfLocalConcepts();
        try {
            ec.writeGrib1Tables();
            System.out.println("Finished!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
