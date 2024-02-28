package ser;

import com.ser.blueline.CopyScope;
import com.ser.blueline.IDocument;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.ILink;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.foldermanager.IFolder;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;


public class QADocArchive extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    IProcessInstance processInstance;
    IInformationObject qaInfObj;
    ProcessHelper helper;
    ITask task;
    IDocument document;
    IDocument newDoc;
    List<ILink> attachLinks;
    String compCode;
    String docNr;
    String reqId;
    @Override
    protected Object execute() {
        if (getEventTask() == null)
            return resultError("Null Document object");

        if(getEventTask().getProcessInstance().findLockInfo().getOwnerID() != null){
            return resultRestart("Restarting Agent");
        }

        Utils.session = getSes();
        Utils.bpm = getBpm();
        Utils.server = Utils.session.getDocumentServer();
        Utils.loadDirectory(Conf.Paths.MainPath);
        
        task = getEventTask();

        try {

            helper = new ProcessHelper(Utils.session);
            XTRObjects.setSession(Utils.session);


            processInstance = task.getProcessInstance();
            compCode = (processInstance != null ? Utils.compCode(processInstance) : "");
            if(compCode.isEmpty()){
                throw new Exception("QA-Workspace no is empty.");
            }
            qaInfObj = Utils.getQAWorkspace(compCode, helper);
            if(qaInfObj == null){
                throw new Exception("QA-Worksapce not found [" + compCode + "].");
            }

            reqId = Utils.getQAReqId(qaInfObj, processInstance);
            docNr = Utils.getQADocNr(qaInfObj, processInstance);

            document = (IDocument) processInstance.getMainInformationObject();
            attachLinks = processInstance.getLoadedInformationObjectLinks().getLinks();
            if(document == null){
                document = (!attachLinks.isEmpty() ? (IDocument) attachLinks.get(0).getTargetInformationObject() : null);
                if(document != null) {
                    processInstance.setMainInformationObjectID(document.getID());
                }
            }
            if(document == null){
                throw new Exception("QA-Document not found.");
            }

            document.setDescriptorValue(Conf.Descriptors.Status, "ARCHIVED");
            document.setDescriptorValue(Conf.Descriptors.ReqLastId, reqId);

            document.commit();

            processInstance.commit();
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