package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.QADocReqInit

class TEST_QADocReqInit {

    Binding binding

    @BeforeClass
    static void initSessionPool() {
        AgentTester.initSessionPool()
    }

    @Before
    void retrieveBinding() {
        binding = AgentTester.retrieveBinding()
    }

    @Test
    void testForAgentResult() {
        def agent = new QADocReqInit()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST06BPM_QA24b45dad75-0030-4292-8721-d505e90dd35b182024-02-26T09:41:11.830Z010"

        def result = (AgentExecutionResult)agent.execute(binding.variables)
        assert result.resultCode == 0
    }

    @Test
    void testForJavaAgentMethod() {
        //def agent = new JavaAgent()
        //agent.initializeGroovyBlueline(binding.variables)
        //assert agent.getServerVersion().contains("Linux")
    }

    @After
    void releaseBinding() {
        AgentTester.releaseBinding(binding)
    }

    @AfterClass
    static void closeSessionPool() {
        AgentTester.closeSessionPool()
    }
}
