package com.soebes.maven.plugins.mlv.licenses;

import com.soebes.maven.plugins.mlv.model.LicensesContainer;
import com.soebes.maven.plugins.mlv.model.LicenseItem;
import com.soebes.maven.plugins.mlv.model.LicensesList;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created with IntelliJ IDEA.
 * User: lbennett
 * Date: 17/04/2012
 * Time: 11:45
 * To change this template use File | Settings | File Templates.
 */
public class SourceFileProcessor {

    private ArtifactRepository repository;
    private LicensesList licensesList;
    private static final int TEMP_DIR_ATTEMPTS = 10000;

    public SourceFileProcessor(ArtifactRepository remoteRepository, LicensesContainer container) {
        this.repository = remoteRepository;
        this.licensesList = container.getValid();

        for(LicenseItem li : container.getInvalid().getLicenses()) {
           this.licensesList.addLicense(li);
        }
    }

    public List<?> processArtifact(Artifact artifact) {
        String baseURL = this.repository.getUrl();

        String groupURL = artifact.getGroupId().replaceAll("\\.","/");

        String version = artifact.getVersion();
        version = version.replaceAll(".bundle","");

        String sourceJar = artifact.getArtifactId() + "-" + version + "-sources.jar";
        String downloadURL =  baseURL + "/" + groupURL + "/" + artifact.getArtifactId() + "/" + version + "/" + sourceJar;

        System.out.println("FUBAR: " + downloadURL);

        File tmpDir = createTempDir();
        String output = tmpDir + File.separator + sourceJar;

        ArrayList licenses = new ArrayList();

        try {
            downloadJar(output, downloadURL);
            unzipJar(new File(output), tmpDir.getAbsolutePath());

            System.out.println("Starting to process source files ...");
            System.out.println(artifact);

            List list = processSourceFiles(tmpDir);

            licenses.addAll(list);

            //deleteTempDir(tmpDir);

        } catch (FileNotFoundException fe) {
           System.out.println("No source jar found for: " + artifact.getArtifactId());
           System.out.println(output);
           System.out.println(downloadURL);
           fe.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return licenses;
    }


    private List<License> processSourceFiles(File file)
    {
        List<License> licenses = new ArrayList<License>();

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                if (".".equals(child.getName()) || "..".equals(child.getName())) {
                    continue;  // Ignore the self and parent aliases.
                }

                if(child.isDirectory()) {
                   licenses.addAll(processSourceFiles(child));
                } else if (child.getName().endsWith(".java") || child.getName().endsWith(".txt")){
                    try {
                        StringBuffer buf = getLicenseHeader(child);

                        boolean foundValid = false;
                        int validLicense = 999;
                        int validName = 999;
                        int validURL = 999;

                        for(int a=0; a<licensesList.getLicenses().size(); a++) {

                            LicenseItem item = licensesList.getLicenses().get(a);

                            for(int b=0; b<item.getNames().size(); b++) {
                                String name = item.getNames().get(b);

                                // TODO replace with regex matching
                                if (buf.toString().contains(name)) {
                                    //System.out.println("Found license: " + name);
                                    validLicense = a;
                                    validName = b;
                                    foundValid = true;
                                }
                            }

                            for(int c=0; c<item.getUrls().size(); c++) {
                                String url = item.getUrls().get(c);
                                if (buf.toString().contains(" " + url + " ")) {
                                    //System.out.println("Found license: " + url);
                                    validLicense = a;
                                    validURL = c;
                                    foundValid = true;
                                }
                            }
                        }

                        if(foundValid) {
                            License li = new License();
                            if(validName < 999) {
                                String name = licensesList.getLicenses().get(validLicense).getNames().get(validName);
                                //System.out.println("License name: " + name);
                                if(!name.trim().equals("")) {
                                    li.setName(name);
                                    //System.out.println("Setting name");
                                }
                            }

                            if (validURL < 999) {
                                String url = licensesList.getLicenses().get(validLicense).getUrls().get(validURL);
                                String name = licensesList.getLicenses().get(validLicense).getNames().get(0);
                                //System.out.println("License url: " + url);
                                if(!url.trim().equals("")) {
                                    li.setUrl(url);
                                    li.setName(name);
                                    //System.out.println("Setting url");
                                }
                                li.setUrl(url);
                            }

                            if(li.getName() != null ) {
                                if(!containsLicense(licenses,li)) {
                                    //System.out.println("Adding License to list");
                                    licenses.add(li);
                                }
                            }
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    continue;
                }
            }
        }
        //System.out.println("Licenses Size: " + licenses.size());
        return licenses;
    }

    private boolean containsLicense(List<License> licenses, License li) {
        boolean found = false;
        for (License lic : licenses) {
            //TODO: also check URL
            if(lic.getName().equals(li.getName())) {
                found = true;
                break;
            }
        }
        return found;
    }

    // get header from source file (everything above package) + remove crud
    private StringBuffer getLicenseHeader(File sourceFile) throws IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(
                new FileReader(sourceFile.getAbsolutePath()));
        String strLine;
        while((strLine=reader.readLine()) != null){
            if (strLine.startsWith("package")) {
                break;
            } else {
                if(strLine.length() != 0) {
                    fileData.append(strLine);
                }
            }
        }
        reader.close();
        return fileData;
    }

    private void downloadJar(String output, String downloadURL) throws FileNotFoundException, IOException {
            //System.out.println("Downloading " + downloadURL);
            URL url = new URL(downloadURL);
            url.openConnection();
            InputStream reader = url.openStream();

            FileOutputStream writer = new FileOutputStream(output);
            byte[] buffer = new byte[153600];
            int totalBytesRead = 0;
            int bytesRead = 0;

            while ((bytesRead = reader.read(buffer)) > 0)
            {
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[153600];
                totalBytesRead += bytesRead;
            }
            writer.close();
            reader.close();
            System.out.println("finished downloading");
    }

    private void unzipJar(File jarFile, String destDir) throws IOException {
        System.out.println("UNZIPPING JAR");
        JarFile jar = new JarFile(jarFile);
        Enumeration enumeration = jar.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry file = (JarEntry) enumeration.nextElement();
            File f = new File(destDir + File.separator + file.getName());
            if (file.isDirectory()) { // if its a directory, create it
                f.mkdirs();
                continue;
            }
            InputStream is = jar.getInputStream(file); // get the input stream
            FileOutputStream fos = new FileOutputStream(f);
            while (is.available() > 0) {  // write contents of 'is' to 'fos'
                fos.write(is.read());
            }
            fos.close();
            is.close();
        }
        jarFile.delete();
    }

    private static File createTempDir() {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                System.out.println("crated tempDir: " + tempDir.getAbsolutePath());
                return tempDir;
            }
        }
        throw new IllegalStateException("Failed to create directory within "
                + TEMP_DIR_ATTEMPTS + " attempts (tried "
                + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
    }

    private static void deleteTempDir(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException("Not a directory " + dir);
        }

        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if (file.isDirectory()) {
                deleteTempDir(file);
            }
            else {
                boolean deleted = file.delete();
                if (!deleted) {
                    throw new IOException("Unable to delete file" + file);
                }
            }
        }

        dir.delete();
    }
}
