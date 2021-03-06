/**
 * The Maven License Verifier Plugin
 *
 * Copyright (c) 2009, 2010, 2011 by SoftwareEntwicklung Beratung Schulung (SoEBeS)
 * Copyright (c) 2009, 2010, 2011 by Karl Heinz Marbaise
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.soebes.maven.plugins.mlv.licenses;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.soebes.maven.plugins.mlv.model.LicensesContainer;
import com.soebes.maven.plugins.mlv.model.io.xpp3.LicensesXpp3Reader;
import com.soebes.maven.plugins.mlv.model.io.xpp3.LicensesXpp3Writer;

/**
 * This class will serve methods for loading the licenses.xml file.
 *
 * @author <a href="mailto:kama@soebes.de">Karl Heinz Marbaise</a>
 *
 */
public class LicensesFile {

    /**
     * Load the licenses.xml file and convert it into
     * LicenseContainer class to have the information of the licenses.xml
     * file available.
     * @param licenseFile
     * @return LicensesContainer instance.
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static LicensesContainer getLicenses(File licenseFile) throws IOException, XmlPullParserException {
        return getLicenses(new FileInputStream(licenseFile));
    }

    /**
     * Load the licenses.xml file from an InputStream which
     * usually means via getResourceAsStream().
     *
     * @param licensesInputStream The input stream where to load the license file from.
     * @return LicensesContainer instance.
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static LicensesContainer getLicenses(InputStream licensesInputStream) throws IOException, XmlPullParserException {
        LicensesXpp3Reader read = new LicensesXpp3Reader();
        LicensesContainer licenses = read.read(licensesInputStream);
        return licenses;
    }

    /**
     * We convert the LicensesContainer into a string.
     *
     * @param licensesContainer
     * @return
     * @throws IOException
     */
    public static String toString(LicensesContainer licensesContainer) throws IOException {
        StringWriter stringWriter = new StringWriter();
        LicensesXpp3Writer xmlWriter = new LicensesXpp3Writer();
        xmlWriter.write(stringWriter, licensesContainer);
        return stringWriter.toString();
    }
}
