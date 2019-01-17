package works.weave.socks.queuemaster.loadtest;

import com.google.common.collect.ImmutableList;
import com.neotys.neoload.model.repository.*;
import com.neotys.testing.framework.BaseNeoLoadDesign;
import com.neotys.testing.framework.BaseNeoLoadUserPath;

import java.util.List;

import static java.util.Collections.emptyList;

public class BasicUserPath extends BaseNeoLoadUserPath {
    public BasicUserPath(BaseNeoLoadDesign design) {
        super(design);
    }

    @Override
    public UserPath createVirtualUser(BaseNeoLoadDesign baseNeoLoadDesign) {
        final Server server = baseNeoLoadDesign.getServerByName("Server");
        final ConstantVariable constantpath= (ConstantVariable) baseNeoLoadDesign.getVariableByName("basicPath");

        final Request getRequest = getBuilder(server, variabilize(constantpath), emptyList(),emptyList(),emptyList()).build();

        final Delay delay = thinkTime(250);
        final ImmutableContainerForMulti actionsContainer = actionsContainerBuilder()
                .addChilds(container("BasicCheck", getRequest, delay))
                .build();

        return userPathBuilder("BasicCheckTesting")
                .actionsContainer(actionsContainer)
                .build();


    }
}
