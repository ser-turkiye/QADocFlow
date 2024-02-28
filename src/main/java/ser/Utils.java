package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.*;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.foldermanager.*;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utils {
    static Logger log = LogManager.getLogger();
    static ISession session = null;
    static IDocumentServer server = null;
    static IBpmService bpm;

    static void loadDirectory(String path) {
        (new File(path)).mkdir();
    }
    public static String compCode(IInformationObject projectInfObj) throws Exception {
        String rtrn = "";
        if(Utils.hasDescriptor(projectInfObj, Conf.Descriptors.CompCode)){
            rtrn = projectInfObj.getDescriptorValue(Conf.Descriptors.CompCode, String.class);
            rtrn = (rtrn == null ? "" : rtrn).trim();
        }
        return rtrn;
    }
    public static IDocument createQADocument()  {
        IArchiveClass ac = server.getArchiveClass(Conf.ClassIDs.QADocument, session);
        IDatabase db = session.getDatabase(ac.getDefaultDatabaseID());

        return server.getClassFactory().getDocumentInstance(db.getDatabaseName(), ac.getID(), "0000" , session);
    }
    static IInformationObject getQAWorkspace(String comp, ProcessHelper helper) {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.QAWorkspace).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.CompCode).append(" = '").append(comp).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);

        IInformationObject[] informationObjects = helper.createQuery(new String[]{Conf.Databases.QAWorkspace}, whereClause, "", 1, false);
        if(informationObjects.length < 1) {return null;}
        return informationObjects[0];
    }
    static IInformationObject[] getQADocuments(String comp, String docNo, ProcessHelper helper) {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.QADocument).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.CompCode).append(" = '").append(comp).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.DocNo).append(" = '").append(docNo).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);

        IInformationObject[] informationObjects = helper.createQuery(new String[]{Conf.Databases.QAWorkspace}, whereClause, "", 0, false);
        return informationObjects;
    }
    public static boolean hasDescriptor(IInformationObject object, String descName){
        IDescriptor[] descs = session.getDocumentServer().getDescriptorByName(descName, session);
        List<String> checkList = new ArrayList<>();
        for(IDescriptor ddsc : descs){checkList.add(ddsc.getId());}

        String[] descIds = new String[0];
        if(object instanceof IFolder){
            String classID = object.getClassID();
            IArchiveFolderClass folderClass = session.getDocumentServer().getArchiveFolderClass(classID , session);
            descIds = folderClass.getAssignedDescriptorIDs();
        } else if(object instanceof IDocument){
            IArchiveClass documentClass = ((IDocument) object).getArchiveClass();
            descIds = documentClass.getAssignedDescriptorIDs();
        } else if(object instanceof ITask){
            IProcessType processType = ((ITask) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        } else if(object instanceof IProcessInstance){
            IProcessType processType = ((IProcessInstance) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        }

        List<String> descList = Arrays.asList(descIds);
        for(String dId : descList){if(checkList.contains(dId)){return true;}}
        return false;
    }
    public static boolean connectToFolder(IFolder folder, String rootName, String fold, IInformationObject pdoc) throws Exception {
        boolean add2Node = false;
        List<INode> nodes = folder.getNodesByName(rootName);
        if(nodes.isEmpty()){return false;}

        INodes root = (INodes) nodes.get(0).getChildNodes();
        INode fnod = root.getItemByName(fold);
        if(fnod == null) {return false;}

        boolean isExistElement = false;
        String pdocID = pdoc.getID();
        IElements nelements = fnod.getElements();
        for(int i=0;i<nelements.getCount2();i++) {
            IElement nelement = nelements.getItem2(i);
            String edocID = nelement.getLink();
            if(Objects.equals(pdocID, edocID)){
                isExistElement = true;
                break;
            }
        }
        if(isExistElement) {return false;}

        add2Node = folder.addInformationObjectToNode(pdoc.getID(), fnod.getID());

        folder.commit();
        return add2Node;
    }
    public static boolean disconnectToFolder(IFolder folder, String rootName, String fold, IInformationObject pdoc) throws Exception {
        boolean add2Node = false;
        List<INode> nodes = folder.getNodesByName(rootName);
        if(nodes.isEmpty()){return false;}

        INodes root = (INodes) nodes.get(0).getChildNodes();
        INode fnod = root.getItemByName(fold);
        if(fnod == null) {return false;}

        int rIndex = -1;
        String pdocID = pdoc.getID();
        IElements nelements = fnod.getElements();
        for(int i=0;i<nelements.getCount2();i++) {
            IElement nelement = nelements.getItem2(i);
            String edocID = nelement.getLink();
            if(!Objects.equals(pdocID, edocID)){
                continue;
            }
            nelements.remove(i);
            rIndex = i;
        }
        if(rIndex<0) {return false;}

        folder.commit();
        return add2Node;
    }
    public static String getQADocId(IInformationObject qaws, IDocument document) throws Exception {
        String rtrn = document.getDescriptorValue(Conf.Descriptors.DocId, String.class);
        rtrn = (rtrn == null ? "" : rtrn.trim());
        if(rtrn.isEmpty()) {

            String compCode = "";
            if(Utils.hasDescriptor(document, Conf.Descriptors.CompCode)){
                compCode = document.getDescriptorValue(Conf.Descriptors.CompCode, String.class);
                compCode = (compCode == null ? "" : compCode).trim();
            }
            String docStts = "";
            if(Utils.hasDescriptor(document, Conf.Descriptors.Status)){
                docStts = document.getDescriptorValue(Conf.Descriptors.Status, String.class);
                docStts = (docStts == null ? "" : docStts).trim();
            }

            String cntPattern = "";
            if(docStts.equals("IMPORT") && Utils.hasDescriptor(qaws, Conf.Descriptors.PatternImpNr)){
                cntPattern = qaws.getDescriptorValue(Conf.Descriptors.PatternImpNr, String.class);
                cntPattern = (cntPattern == null ? "" : cntPattern).trim();
            }
            if(docStts.equals("DRAFT") && Utils.hasDescriptor(qaws, Conf.Descriptors.PatternDrfNo)){
                cntPattern = qaws.getDescriptorValue(Conf.Descriptors.PatternDrfNo, String.class);
                cntPattern = (cntPattern == null ? "" : cntPattern).trim();
            }

            if(!cntPattern.isEmpty()
                    && !compCode.isEmpty()
                    && !docStts.isEmpty()){
                String counterName = AutoText.init().with(document)
                        .param("Company", compCode)
                        .param("Status", docStts)
                        .run("QA.{Company}.{Status}");

                NumberRange nr = new NumberRange();
                if(!nr.has(counterName)){
                    nr.append(counterName, cntPattern, 0L);
                }

                nr.parameter("Company", compCode);
                nr.parameter("Status", docStts);
                rtrn = nr.increment(counterName, cntPattern);
            }

            document.setDescriptorValue(Conf.Descriptors.DocId,
                    rtrn);
        }
        return rtrn;
    }
    public static String getQAReqId(IInformationObject qaws, IProcessInstance processInstance) throws Exception {
        String rtrn = processInstance.getDescriptorValue(Conf.Descriptors.DocId, String.class);
        rtrn = (rtrn == null ? "" : rtrn.trim());
        if(rtrn.isEmpty()) {

            String compCode = "";
            if(Utils.hasDescriptor(processInstance, Conf.Descriptors.CompCode)){
                compCode = processInstance.getDescriptorValue(Conf.Descriptors.CompCode, String.class);
                compCode = (compCode == null ? "" : compCode).trim();
            }
            String cntPattern = "";
            if(Utils.hasDescriptor(qaws, Conf.Descriptors.PatternReqNo)){
                cntPattern = qaws.getDescriptorValue(Conf.Descriptors.PatternReqNo, String.class);
                cntPattern = (cntPattern == null ? "" : cntPattern).trim();
            }

            String reqType = "Document";
            if(!cntPattern.isEmpty()
                    && !compCode.isEmpty()
                    && !reqType.isEmpty()){
                String counterName = AutoText.init().with(processInstance)
                        .param("Company", compCode)
                        .param("ReqType", reqType)
                        .run("QA.{Company}.Request.{ReqType}");

                NumberRange nr = new NumberRange();
                if(!nr.has(counterName)){
                    nr.append(counterName, cntPattern, 0L);
                }

                nr.parameter("Company", compCode);
                nr.parameter("ReqType", reqType);
                rtrn = nr.increment(counterName, cntPattern);
            }

            processInstance.setDescriptorValue(Conf.Descriptors.DocId,
                    rtrn);
        }
        return rtrn;
    }
    public static String getQADocNr(IInformationObject qaws, IProcessInstance processInstance) throws Exception {
        String rtrn = processInstance.getDescriptorValue(Conf.Descriptors.DocNr, String.class);
        rtrn = (rtrn == null ? "" : rtrn.trim());
        if(rtrn.isEmpty()) {

            String compCode = "";
            if(Utils.hasDescriptor(processInstance, Conf.Descriptors.CompCode)){
                compCode = processInstance.getDescriptorValue(Conf.Descriptors.CompCode, String.class);
                compCode = (compCode == null ? "" : compCode).trim();
            }
            String catgCode = "";
            if(Utils.hasDescriptor(processInstance, Conf.Descriptors.CatgCode)){
                catgCode = processInstance.getDescriptorValue(Conf.Descriptors.CatgCode, String.class);
                catgCode = (catgCode == null ? "" : catgCode).trim();
            }
            String typeCode = "";
            if(Utils.hasDescriptor(processInstance, Conf.Descriptors.TypeCode)){
                typeCode = processInstance.getDescriptorValue(Conf.Descriptors.TypeCode, String.class);
                typeCode = (typeCode == null ? "" : typeCode).trim();
            }

            String cntPattern = "";
            if(Utils.hasDescriptor(qaws, Conf.Descriptors.PatternDocNr)){
                cntPattern = qaws.getDescriptorValue(Conf.Descriptors.PatternDocNr, String.class);
                cntPattern = (cntPattern == null ? "" : cntPattern).trim();
            }

            if(!cntPattern.isEmpty()
            && !compCode.isEmpty()
            && !catgCode.isEmpty()
            && !typeCode.isEmpty()){
                String counterName = AutoText.init().with(processInstance)
                        .param("Company", compCode)
                        .param("Category", catgCode)
                        .param("Type", typeCode)
                        .run("QA.{Company}.{Category}.{Type}");

                NumberRange nr = new NumberRange();
                if(!nr.has(counterName)){
                    nr.append(counterName, cntPattern, 0L);
                }

                nr.parameter("Company", compCode);
                nr.parameter("Category", catgCode);
                nr.parameter("Type", typeCode);
                rtrn = nr.increment(counterName, cntPattern);
            }

            processInstance.setDescriptorValue(Conf.Descriptors.DocNr,
                    rtrn);
        }
        return rtrn;
    }
    public static void sendHTMLMail(JSONObject pars) throws Exception {
        JSONObject mcfg = Utils.getMailConfig();

        String host = mcfg.getString("host");
        String port = mcfg.getString("port");
        String protocol = mcfg.getString("protocol");
        String sender = mcfg.getString("sender");
        String subject = "";
        String mailTo = "";
        String mailCC = "";
        String attachments = "";

        if(pars.has("From")){
            sender = pars.getString("From");
        }
        if(pars.has("To")){
            mailTo = pars.getString("To");
        }
        if(pars.has("CC")){
            mailCC = pars.getString("CC");
        }
        if(pars.has("Subject")){
            subject = pars.getString("Subject");
        }
        if(pars.has("AttachmentPaths")){
            attachments = pars.getString("AttachmentPaths");
        }


        Properties props = new Properties();

        props.put("mail.debug","true");
        props.put("mail.smtp.debug", "true");

        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        String start_tls = (mcfg.has("start_tls") ? mcfg.getString("start_tls") : "");
        if(start_tls.equals("true")) {
            props.put("mail.smtp.starttls.enable", start_tls);
        }

        String auth = mcfg.getString("auth");
        props.put("mail.smtp.auth", auth);
        jakarta.mail.Authenticator authenticator = null;
        if(!auth.equals("false")) {
            String auth_username = mcfg.getString("auth.username");
            String auth_password = mcfg.getString("auth.password");

            if (host.contains("gmail")) {
                props.put("mail.smtp.socketFactory.port", port);
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
            }
            if (protocol != null && protocol.contains("TLSv1.2"))  {
                props.put("mail.smtp.ssl.protocols", protocol);
                props.put("mail.smtp.ssl.trust", "*");
                props.put("mail.smtp.socketFactory.port", port);
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
            }
            authenticator = new jakarta.mail.Authenticator(){
                @Override
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication(){
                    return new jakarta.mail.PasswordAuthentication(auth_username, auth_password);
                }
            };
        }
        props.put("mail.mime.charset","UTF-8");
        Session session = (authenticator == null ? Session.getDefaultInstance(props) : Session.getDefaultInstance(props, authenticator));

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender.replace(";", ",")));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailTo.replace(";", ",")));
        message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(mailCC.replace(";", ",")));
        message.setSubject(subject);

        Multipart multipart = new MimeMultipart("mixed");

        BodyPart htmlBodyPart = new MimeBodyPart();
        htmlBodyPart.setContent(getHTMLFileContent(pars.getString("BodyHTMLFile")), "text/html; charset=UTF-8"); //5
        multipart.addBodyPart(htmlBodyPart);

        String[] atchs = attachments.split("\\;");
        for (String atch : atchs){
            if(atch.isEmpty()){continue;}
            BodyPart attachmentBodyPart = new MimeBodyPart();
            attachmentBodyPart.setDataHandler(new DataHandler((DataSource) new FileDataSource(atch)));

            String fnam = Paths.get(atch).getFileName().toString();
            if(pars.has("AttachmentName." + fnam)){
                fnam = pars.getString("AttachmentName." + fnam);
            }

            attachmentBodyPart.setFileName(fnam);
            multipart.addBodyPart(attachmentBodyPart);

        }

        message.setContent(multipart);
        Transport.send(message);

    }
    public static String getHTMLFileContent (String path) throws Exception {
        String rtrn = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        rtrn = rtrn.replace("\uFEFF", "");
        rtrn = rtrn.replace("ï»¿", "");
        return rtrn;
    }
    public static JSONObject getSystemConfig() throws Exception {
        return getSystemConfig(null);
    }
    public static JSONObject getSystemConfig(IStringMatrix mtrx) throws Exception {
        if(mtrx == null){
            mtrx = server.getStringMatrix("CCM_SYSTEM_CONFIG", session);
        }
        if(mtrx == null) throw new Exception("SystemConfig Global Value List not found");

        List<List<String>> rawTable = mtrx.getRawRows();

        String srvn = session.getSystem().getName().toUpperCase();
        JSONObject rtrn = new JSONObject();
        for(List<String> line : rawTable) {
            String name = line.get(0);
            if(!name.toUpperCase().startsWith(srvn + ".")){continue;}
            name = name.substring(srvn.length() + ".".length());
            rtrn.put(name, line.get(1));
        }
        return rtrn;
    }
    public static JSONObject getMailConfig() throws Exception {
        return getMailConfig(null);
    }
    public static JSONObject getMailConfig(IStringMatrix mtrx) throws Exception {
        if(mtrx == null) {
            mtrx = server.getStringMatrix("CCM_MAIL_CONFIG", session);
        }
        if(mtrx == null) throw new Exception("MailConfig Global Value List not found");
        List<List<String>> rawTable = mtrx.getRawRows();

        JSONObject rtrn = new JSONObject();
        for(List<String> line : rawTable) {
            rtrn.put(line.get(0), line.get(1));
        }
        return rtrn;
    }
    public static JSONObject getWorkbasket( String userID, IStringMatrix mtrx) throws Exception {
        if(mtrx == null){
            mtrx = server.getStringMatrixByID("Workbaskets", session);
        }
        if(mtrx == null) throw new Exception("Workbaskets Global Value List not found");
        List<List<String>> rawTable = mtrx.getRawRows();

        for(List<String> line : rawTable) {
            if(line.contains(userID)) {
                JSONObject rtrn = new JSONObject();
                rtrn.put("ID", line.get(0));
                rtrn.put("Name", line.get(1));
                rtrn.put("DisplayName", line.get(2));
                rtrn.put("Active", line.get(3));
                rtrn.put("Visible", line.get(4));
                rtrn.put("Type", line.get(5));
                rtrn.put("Organization", line.get(6));
                rtrn.put("Access", line.get(7));
                return rtrn;
            }
        }
        return null;
    }
    public static void copyFile(String spth, String tpth) throws Exception {
        FileUtils.copyFile(new File(spth), new File(tpth));
    }
    public static String dateToString(Date dval) throws Exception {
        if(dval == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy").format(dval);
    }
    public static String zipFiles(String zipPath, String pdfPath, List<String> expFilePaths) throws IOException {
        if(expFilePaths.size() == 0){return "";}

        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(new File(zipPath)));
        if(!pdfPath.isEmpty()) {
            //ZipEntry zltp = new ZipEntry("00." + Paths.get(tpltSavePath).getFileName().toString());
            ZipEntry zltp = new ZipEntry("_Correspondence." + FilenameUtils.getExtension(pdfPath));
            zout.putNextEntry(zltp);
            byte[] zdtp = Files.readAllBytes(Paths.get(pdfPath));
            zout.write(zdtp, 0, zdtp.length);
            zout.closeEntry();
        }

        for (String expFilePath : expFilePaths) {
            String fileName = Paths.get(expFilePath).getFileName().toString();
            fileName = fileName.replace("[@SLASH]", "/");
            ZipEntry zlin = new ZipEntry(fileName);

            zout.putNextEntry(zlin);
            byte[] zdln = Files.readAllBytes(Paths.get(expFilePath));
            zout.write(zdln, 0, zdln.length);
            zout.closeEntry();
        }
        zout.close();
        return zipPath;
    }
    public static String exportDocument(IDocument document, String exportPath, String fileName) throws IOException {
        String rtrn ="";
        IDocumentPart partDocument = document.getPartDocument(document.getDefaultRepresentation() , 0);
        String fName = (!fileName.isEmpty() ? fileName : partDocument.getFilename());
        fName = fName.replaceAll("[\\\\/:*?\"<>|]", "_");

        try (InputStream inputStream = partDocument.getRawDataAsStream()) {
            IFDE fde = partDocument.getFDE();
            if (fde.getFDEType() == IFDE.FILE) {
                rtrn = exportPath + "/" + fName + "." + ((IFileFDE) fde).getShortFormatDescription();

                try (FileOutputStream fileOutputStream = new FileOutputStream(rtrn)){
                    byte[] bytes = new byte[2048];
                    int length;
                    while ((length = inputStream.read(bytes)) > -1) {
                        fileOutputStream.write(bytes, 0, length);
                    }
                }
            }
        }
        return rtrn;
    }
    public static String exportRepresentation(IDocument document, int rinx, String exportPath, String fileName) throws IOException {
        String rtrn ="";
        IDocumentPart partDocument = document.getPartDocument(rinx , 0);
        String fName = (!fileName.isEmpty() ? fileName : partDocument.getFilename());
        fName = fName.replaceAll("[\\\\/:*?\"<>|]", "_");
        try (InputStream inputStream = partDocument.getRawDataAsStream()) {
            IFDE fde = partDocument.getFDE();
            if (fde.getFDEType() == IFDE.FILE) {
                rtrn = exportPath + "/" + fName + "." + ((IFileFDE) fde).getShortFormatDescription();

                try (FileOutputStream fileOutputStream = new FileOutputStream(rtrn)){
                    byte[] bytes = new byte[2048];
                    int length;
                    while ((length = inputStream.read(bytes)) > -1) {
                        fileOutputStream.write(bytes, 0, length);
                    }
                }
            }
        }
        return rtrn;
    }

}
