module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires com.google.gson;
    requires java.sql;
    requires miglayout.swing;
    requires miglayout.core;
    opens com.udacity.catpoint.security.data to com.google.gson;
    opens com.udacity.catpoint.security.service to org.junit.jupiter.api;
}