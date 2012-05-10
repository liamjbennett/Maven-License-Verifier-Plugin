package com.soebes.maven.plugins.mlv.licenses;

import org.apache.maven.model.License;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lbennett
 * Date: 30/04/2012
 * Time: 11:18
 * To change this template use File | Settings | File Templates.
 */
public class LicenseMapping {
    private List<String> sources;
    private License license;

    public LicenseMapping(List<String> sources, License license) {
       this.sources = sources;
       this.license = license;
    }

    public LicenseMapping(License license) {
        this.license = license;
    }

    public static List<String> getPomSource() {
        List<String> pomSource = new ArrayList<String>();
        pomSource.add("pom.xml");
        return pomSource;
    }

    public License getLicense() {
        return license;
    }

    public List<String> getSources() {
        return sources;
    }
}
