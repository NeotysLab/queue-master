package works.weave.socks.queuemaster.loadtest;

import com.neotys.testing.framework.BaseNeoLoadDesign;
import com.neotys.testing.framework.NeoLoadTest;

public class QueueMasterLoadTest extends NeoLoadTest {
    @Override
    protected BaseNeoLoadDesign design() {
        return new TestingDesign();
    }

    @Override
    protected String projectName() {
        return "queuemaster_NeoLoad";
    }

    @Override
    public void createComplexPopulation() {

    }

    @Override
    public void createComplexScenario() {

    }

    @Override
    public void execute() {

        createSimpleConstantLoadScenario("QueueMaster_Load","BasicCheckTesting",600,49,10);
        createSimpleConstantIterationScenario("DynatraceSanityCheck","BasicCheckTesting",15,1,0);
        createSanityCheckScenario();
    }
}
