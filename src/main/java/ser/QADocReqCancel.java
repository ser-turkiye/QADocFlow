package ser;

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


public class QADocReqCancel extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    IProcessInstance processInstance;
    IInformationObject qaInfObj;
    ProcessHelper helper;
    ITask task;
    IInformationObject document;
    String compCode;
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
                throw new Exception("QA-Workspace not found [" + compCode + "].");
            }

            document = processInstance.getMainInformationObject();
            if(document == null){
                List<ILink> aLnks = processInstance.getLoadedInformationObjectLinks().getLinks();
                document = (!aLnks.isEmpty() ? aLnks.get(0).getTargetInformationObject() : document);
                if(document != null) {
                    processInstance.setMainInformationObjectID(document.getID());
                }
            }
            if(document == null){
                throw new Exception("QA-Document not found.");
            }

            String dsts = document.getDescriptorValue(Conf.Descriptors.Status, String.class);
            dsts = (dsts == null? "" : dsts);

            String pctg = processInstance.getDescriptorValue(Conf.Descriptors.ProcCatg, String.class);
            pctg = (pctg == null? "" : pctg);

            if(pctg.equals("Publish") && dsts.equals("WA4APRV")) {
                document.setDescriptorValue(Conf.Descriptors.Status, "DRAFT");
                document.commit();
            }

            reqId = Utils.getQAReqId(qaInfObj, processInstance);
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