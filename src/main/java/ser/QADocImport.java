package ser;

import com.ser.blueline.IDocument;
import com.ser.blueline.IInformationObject;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;


public class QADocImport extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    IInformationObject qaInfObj;
    ProcessHelper helper;
    IDocument document;
    String compCode;
    String docId;
    @Override
    protected Object execute() {
        if (getEventDocument() == null)
            return resultError("Null Document object");

        Utils.session = getSes();
        Utils.bpm = getBpm();
        Utils.server = Utils.session.getDocumentServer();
        Utils.loadDirectory(Conf.Paths.MainPath);
        
        document = getEventDocument();

        try {

            helper = new ProcessHelper(Utils.session);
            XTRObjects.setSession(Utils.session);


            compCode = (document != null ? Utils.compCode(document) : "");
            if(compCode.isEmpty()){
                throw new Exception("QA-Workspace no is empty.");
            }
            qaInfObj = Utils.getQAWorkspace(compCode, helper);
            if(qaInfObj == null){
                throw new Exception("QA-Workspace not found [" + compCode + "].");
            }

            docId = Utils.getQADocId(qaInfObj, document);

            document.commit();
            //processInstance.commit();
            log.info("Tested.");

        } catch (Exception e) {
            //throw new RuntimeException(e);
            log.error("Exception       : " + e.getMessage());
            log.error("    Class       : " + e.getClass());
            log.error("    Stack-Trace : " + Arrays.toString(e.getStackTrace()));
            return resultError("Exception : " + e.getMessage());
        }

        log.info("Finished");
        return resultSuccess("Ended successfully");
    }
}