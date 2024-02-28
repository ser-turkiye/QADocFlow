package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.QADocDraft

class TEST_QADocDraft {

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
        def agent = new QADocDraft()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST06BPM_QA24d8278922-0dd2-4144-856e-eb2af860de23182024-02-25T07:08:50.193Z010"

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
