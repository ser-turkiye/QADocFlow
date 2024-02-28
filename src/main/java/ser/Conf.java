package ser;

import org.json.JSONObject;

import java.util.Date;

public class Conf {

    public static class Paths {
        public static final String MainPath = "C:/tmp2/bulk/qa-flow";
    }
    public static class Descriptors{
        public static final String PatternDocNr = "PatternDocumentNo";
        public static final String PatternImpNr = "PatternImportNo";
        public static final String PatternDrfNo = "PatternDraftNo";
        public static final String PatternReqNo = "PatternRequestNo";
        public static final String CompCode = "OrgCompanyDescription";
        public static final String CatgCode = "ObjectCategoryName";
        public static final String TypeCode = "ObjectType";
        public static final String DocId = "ObjectDocID";
        public static final String DocNr = "ObjectNumber";
        public static final String RevNr = "ObjectNumberExternal";
        public static final String Status = "ObjectStatus";
        public static final String ReqLastId = "ReqLastId";
        public static final String ReqPrevId = "ReqPrevId";

    }

    public static class DescriptorLiterals{
        public static final String CompCode = "ORGCOMPANYDESCRIPTION";
        public static final String DocNo = "OBJECTNUMBER";

    }
    public static class ClassIDs{
        public static final String QADocument = "050b018f-b6ef-4c9c-a819-a76a5309f173";
        public static final String QAWorkspace = "5e664e59-3c81-48dd-925e-4e731f12ca65";

    }

    public static class Databases{
        public static final String QAWorkspace = "D_QA";

    }
    public static class Bookmarks{


        public  static final JSONObject projectWorkspaceTypes() {
            JSONObject rtrn = new JSONObject();
            rtrn.put("DurDay", Integer.class);
            rtrn.put("DurHour", Double.class);

            rtrn.put("IssueDate", Date.class);
            rtrn.put("ApprovedDate", Date.class);
            rtrn.put("OriginatedDate", Date.class);


            JSONObject dbks = distribution();
            JSONObject dbts = distributionTypes();

            for (String dkey : dbks.keySet()) {
                if(!dbts.has(dkey)){continue;}
                for(int p=1;p<=5;p++){
                    String dinx = (p <= 9 ? "0" : "") + p;
                    rtrn.put(dkey + dinx, dbts.get(dkey));
                }
            }

            return rtrn;
        }
        public  static final JSONObject projectWorkspace() {
            JSONObject rtrn = new JSONObject();
            rtrn.put("ProjectNo", "ccmPRJCard_code");
            rtrn.put("ProjectName", "ccmPRJCard_name");
            rtrn.put("CrspType", "ccmCrspDocType");
            rtrn.put("To", "To-Receiver");
            rtrn.put("Attention", "ObjectAuthors");
            rtrn.put("CC", "CC-Receiver");
            //rtrn.put("JobNo", "JobNo");
            rtrn.put("CorrespondenceNo", "ccmPrjDocNumber");
            rtrn.put("IssueDate", "DateStart");
            rtrn.put("Discipline", "");
            rtrn.put("Summary", "ccmCrspSummary");
            rtrn.put("Notes", "ccmCrspNotes");
            rtrn.put("DurDay", "ccmPrjProcDurDay");
            rtrn.put("DurHour", "ccmPrjProcDurHour");

            rtrn.put("Approved", "ccmApproved");
            rtrn.put("ApprovedDate", "ccmApprovedDate2");
            rtrn.put("Originated", "ccmOriginated");
            rtrn.put("OriginatedDate", "ccmOriginatedDate2");

            rtrn.put("SenderName", "ccmCrspSender");
            rtrn.put("SenderCode", "ccmSenderCode");
            rtrn.put("ReceiverName", "ccmCrspReceiver");
            rtrn.put("ReceiverCode", "ccmReceiverCode");

            JSONObject ebks = engDocument();
            for (String ekey : ebks.keySet()) {
                for(int p=1;p<=50;p++){
                    String einx = ekey + (p <= 9 ? "0" : "") + p;
                    rtrn.put(einx, "");
                }
            }

            JSONObject dbks = distribution();
            for (String dkey : dbks.keySet()) {
                String dfkl = dbks.getString(dkey);
                for(int p=1;p<=5;p++){
                    String dinx = (p <= 9 ? "0" : "") + p;
                    rtrn.put(dkey + dinx, dfkl.replace("##", dinx));
                }
            }
            rtrn.put("IssueStatus", "");
            rtrn.put("IssueType", "ObjectType");

            rtrn.put("OrigndFullname", "");
            rtrn.put("OrigndJobTitle", "");
            rtrn.put("OrigndDate", "");
            rtrn.put("OrigndSignature", "");

            rtrn.put("ApprvdFullname", "");
            rtrn.put("ApprvdJobTitle", "");
            rtrn.put("ApprvdDate", "");
            rtrn.put("ApprvdSignature", "");

            return rtrn;
        }

        public static final String DistributionMaster = "DistUser";
        public  static final JSONObject distribution() {
            JSONObject rtrn = new JSONObject();
            rtrn.put("DistUser", "ccmDistUser##");
            rtrn.put("DistPurpose", "ccmDistPurpose##");
            rtrn.put("DistDlvMethod", "ccmDistDlvMethod##");
            rtrn.put("DistDueDate", "ccmDistDueDate##");
            return rtrn;
        }
        public  static final JSONObject distributionTypes() {
            JSONObject rtrn = new JSONObject();
            rtrn.put("DistDueDate", Date.class);
            return rtrn;
        }
        public static final String EngDocumentMaster = "DocNo";
        public  static final JSONObject engDocument() {
            JSONObject rtrn = new JSONObject();
            rtrn.put("DocNo", "ccmPrjDocNumber");
            rtrn.put("RevNo", "ccmPrjDocRevision");
            rtrn.put("ParentDoc", "");
            rtrn.put("Desc", "ObjectName");
            rtrn.put("Issue", "ccmPrjDocIssueStatus");
            rtrn.put("FileName", "ccmPrjDocFileName");
            rtrn.put("Remarks", "");

            return rtrn;
        }

    }


}
