---
title: 2018 Training Workshop Schedule
last_updated: 2018-10-27
sidebar: tdsTutorial_sidebar
toc: false
permalink:  workshop2018.html
---

## 2018-10-25 (Thursday)

### 8:30 (30 minutes) Coffee and continental breakfast

### 9:00 (30 minutes) Welcome and Logistics / TDS Overview (Sean)
* Introduction of Unidata staff
* Review of schedule
* Why use TDS? ({% include link_file.html file="workshop_presentations/2018/TDSOverview.pdf" text="pdf" %})
* [Quick notes](https://docs.google.com/document/d/1mn-OgFPN4fN21ZnkmXNAI8iwFzE5qFJqJ91kdGoOJFI/edit?usp=sharing){:target="_blank"}

### 9:30 (30 minutes) Participant Introductions (Sean)
* Name, organization, how you use TDS.

### 10:00 (30 minutes) Getting Started With the TDS: Local Test Server Setup (Jen)
* This section covers basic installation and configuration of Tomcat, JDK and the TDS for a local test server.
  * [Installing Java and Tomcat](install_java_tomcat.html)
  * [Tomcat Directory Structure: Quick Tour](tomcat_dir_structure_qt.html)
  * [Running Tomcat](running_tomcat.html)
  * [Tomcat Log Files](tomcat_log_files.html)
  * [Tomcat (Server-Level) Configuration Files](tomcat_configuration_files.html)
  * [Deploying the TDS](deploying_the_tds.html)
  * [Tomcat Manager Application](tomcat_manager_app.html)
  * [Next Steps: Where To Go From Here](where_to_go_from_here.html)

### 10:30 (15 minutes) Break

### 10:45 (45 minutes) Getting Started With the TDS: Local Test Server Setup part 2 (Jen)
* This section covers basic installation and configuration of Tomcat, JDK and the TDS for a local test server.
  * [Deploying the TDS](deploying_the_tds.html)
  * [Tomcat manager Application](tomcat_manager_app.html)
  * [TDS Remote Management](remote_management_ref.html)
  * [Next Steps: Where To Go From Here](where_to_go_from_here.html)

### 11:30 (1 hour) Lunch in FL-2 Cafeteria

### 12:30 (45 minutes) Configuring TDS (Part 1) : Client and Server Catalog Overview (Sean)
* [TDS Client Catalog Overview](basic_client_catalog.html)
* Running [ToolsUI](toolsui_ref.html)
* [Basic TDS Configuration Catalogs](basic_config_catalog.html)

### 1:15 (30 minutes) Tomcat Monitoring and Debugging (Jen)
* This section covers log files generated by Tomcat and the TDS for the purposes of monitoring and debugging:
  * [Logs!](tds_monitoring_and_debugging.html)
  * [Tomcat Access Logs](tds_monitoring_and_debugging.html#tomcat-access-logs)
  * [Log Files Generated by the TDS](tds_monitoring_and_debugging.html#log-files-generated-by-the-tds)

### 1:45 (15 minutes) TDS Monitoring and Debugging (Sean)
* Looking at logs on the server using Remote Management (a.k.a. debug page)
* [Using the TdsMonitor tool](using_the_tdsmonitor_tool.html)

### 2:00 (15 minutes) Break

### 2:15 (30 minutes) Configuring TDS (Part 2) : service, metadata, datasetScan (Sean)
* [TDS Configuration Catalogs](config_catalog.html)
* [Troubleshooting Configuration Catalogs](troubleshooting_problems.html)

### 2:45 (15 minutes) Configuring TDS (Part 3) : threddsConfig.xml (Sean)
* [Basic threddsConfig.xml](basic_tds_configuration.html)
* [threddsConfig Reference](tds_config_ref.html)
* [Services](services_ref.html)
* [enabling writing netCDF-4 files](netcdf4_c_library.html)

### 3:00 (30 minutes) [Docker](https://github.com/Unidata/thredds-docker){:target="_blank"} (Julien)

### 3:30 (30 minutes) DAP Protocol Services (Dennis)
* OPeNDAP DAP2 and DAP4 Protocol Services ({% include link_file.html file="workshop_presentations/2018/NetcdfWorkShop2018-ThreddsDap.pdf" text="pdf" %})
 
### 4:00 Hardware

### 4:10 Discussion and Questions

### Day One Finish

## 2018-10-26 (Friday)

### 8:30 (30 minutes) Coffee and continental breakfast

### 9:00 (15 minutes) More ToolsUI (Sean)
* Viewer, CoordinateSystem, IOSP, and FeatureTypes

### 9:30 (30 minutes) Conventions (Ethan)
 * Conventions Presentation ({% include link_file.html file="workshop_presentations/2018/2018-10-26_TdsTrainingWorkshop_Conventions_CF.pdf" text="pdf" %})
 * [CF Conventions](http://cfconventions.org/){:target="_blank"}

### 10:00 (15 minutes) Break

### 10:15 (15 minutes) NcML modifications (Sean)
* [Basic NcML tutorial](tds_basic_ncml_tutorial.html)

### 10:30 (30 minutes) NcML aggregation (Sean)
* [NcML Aggregation](tds_ncml_aggregation.html)
* [NcML Aggregation Example Problems](ncml_aggregation_examples.html)
* NcML Aggregations vs Feature Collections ([pdf](https://www.unidata.ucar.edu/software/thredds/current/tds/tutorial/files/NcMLvsFeatureCollections.pdf){:target="_blank"})

### 11:00 (30 minutes) [Using NcML in the TDS](using_ncml_in_the_tds.html)

### 11:30 (1 hour) Lunch FL-2 Cafeteria

### 12:30 (30 minutes) Tour of Services (Sean)
* Data Discovery
  * Data discovery systems: ([pdf](https://www.unidata.ucar.edu/software/thredds/current/tds/tutorial/files/metadata_ncISO.pdf){:target="_blank"})
  * [ncIso](iso_metadata.html)
  * exercise- how can we increase our ncISO score?
  * [Attribute Convention for Data Discovery](http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery){:target="_blank"}
* WMS
  * WMS configure, [reference](wms_ref.html)
* NCSS
  * Netcdf Subset Service [configure](adding_ncss.html), [reference](netcdf_subset_service_ref.html)

### 1:00 (45 minutes) Advanced TDS Configuration (Sean)
* [Feature Collections](feature_collections_ref.html)
* [Point Feature Examples](https://thredds.ucar.edu/thredds/catalog/nws/synoptic/ncdecoded/catalog.html){:target="_blank"}
* [Point Feature Collections](pointfeature_collection_ref.html)
* [FMRC Tutorial](fmrc_tutorial.html)

### 1:45 (15 minutes) Break

### 2:00 (1 hour) GRIB Feature Collections (Sean)
* [GRIB Collection Examples](https://thredds-dev.unidata.ucar.edu/thredds/catalog/idd/forecastModels.html){:target="_blank"}
* [GRIB Feature Collection Reference](grib_feature_collections_ref.html)
* [GRIB Feature Collection Config Options](grib_collection_config_ref.html)
* [TDM](tdm_ref.html)
* [GRIB Feature Collection Tutorial](grib_feature_collections.html)
* [GRIB Index redirection](tds_config_ref.html#grib-index-redirection)
* [GRIB](https://i.imgflip.com/omj0i.jpg){:target="_blank"} links of interest:
  * WMO
    * [GRIB-1 / GRIB-2](https://www.wmo.int/pages/prog/www/WMOCodes/Guides/GRIB/GRIB1-Contents.html){:target="_blank"}
    * [GRIB-2](https://www.wmo.int/pages/prog/www/WMOCodes/Guides/GRIB/GRIB2_062006.pdf){:target="_blank"}
  * [NCEP](http://www.nco.ncep.noaa.gov/pmb/docs/on388/){:target="_blank"}
  * {% include link_file.html file="https://doi.org/10.5065/vkan-dp10" text="On the suitability of BUFR and GRIB for archiving data" %} ([tl;dr;](https://www.unidata.ucar.edu/blogs/developer/en/entry/on_the_suitability_of_grib){:target="_blank"})

### 3:00 (30 minutes) Resources and Contributing (Sean)
* [Source](https://github.com/unidata/thredds){:target="_blank"} on GitHub
* [Issue](https://github.com/unidata/thredds/issues){:target="_blank"} Tracking with GitHub
* Maven artifacts on [Nexus](https://artifacts.unidata.ucar.edu/){:target="_blank"} 
* CDM/TDS Nightly Build/Test System (full test suite)
* Continuous Integration on Travis (subset of tests)
* Static code analysis on sonarcloud
* Service status - [UptimeRobot](https://uptimerobot.com/)
* Email Lists: thredds@unidata.ucar.edu; netcdf-java@unidata.ucar.edu
* Support: support-thredds@unidata.ucar.edu; support-netcdf-java@unidata.ucar.edu
* [TdsConfig on github](https://github.com/unidata/TdsConfig){:target="_blank"}
* [Upgrading to 5.0](upgrade_to_5.html)
   
### 4:00 Open Discussion / Participant Systems / Participant feedback (Sean)

### Day Two Finish

## TDSs that have come up in discussion

* [https://hycom.org/dataserver](https://hycom.org/dataserver)
* [https://rda.ucar.edu/](https://rda.ucar.edu/){:target="_blank"}
* [http://thredds.socib.es/thredds/catalog.html](http://thredds.socib.es/thredds/catalog.html){:target="_blank"}
* [https://www.ncei.noaa.gov/thredds/catalog.html](https://www.ncei.noaa.gov/thredds/catalog.html){:target="_blank"}
* [https://thredds.ucar.edu/thredds/catalog.html](https://thredds.ucar.edu/thredds/catalog.html){:target="_blank"}
* [https://thredds-test.unidata.ucar.edu/thredds/catalog/catalog.html](https://thredds-test.unidata.ucar.edu/thredds/catalog/catalog.html){:target="_blank"}