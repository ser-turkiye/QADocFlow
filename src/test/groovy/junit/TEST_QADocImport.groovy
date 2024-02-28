package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.QADocImport

class TEST_QADocImport {

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
        def agent = new QADocImport()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD04D_QA24da4164dd-5674-48f0-ad4c-3aae067c27b8182024-02-24T07:08:26.477Z011"

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
