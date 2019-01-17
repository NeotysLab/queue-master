package works.weave.socks.queuemaster.loadtest;

import com.neotys.neoload.model.repository.ConstantVariable;
import com.neotys.neoload.model.repository.Variable;
import com.neotys.testing.framework.BaseNeoLoadDesign;
import com.neotys.testing.framework.plugin.apm.DynatraceIntegration;
import com.neotys.testing.framework.plugin.apm.sanityCheck.DynatraceSanityCheck;

import java.nio.file.Paths;

import static com.neotys.testing.framework.utils.NeoLoadHelper.createConstantVariable;
import static com.neotys.testing.framework.utils.NeoLoadHelper.createFileVariable;
import static com.neotys.testing.framework.utils.NeoLoadHelper.createServer;

public class TestingDesign extends BaseNeoLoadDesign {
    @java.lang.Override
    public void createNeoLoadUserPaths() {
        this.addVirtualUser(new BasicUserPath(this));
        this.addVirtualUser(new DynatraceSanityCheck(this));
        this.addVirtualUser(new DynatraceIntegration(this));

    }

    @java.lang.Override
    public void createVariables() {
        final ConstantVariable server = createConstantVariable("host", "35.180.172.93");
        final ConstantVariable port = createConstantVariable("port", "80");
        final ConstantVariable cartserver = createConstantVariable("carts_host", "carts.jx-staging.35.233.18.9.nip.io");
        final ConstantVariable cartsport = createConstantVariable("carts_port", "80");
        //TODO take care about file path, perhaps we should use a mechanism to copy the source file to the NeoLoad project folder ?
        final ConstantVariable basicPath = createConstantVariable("basicPath", "/");
        final ConstantVariable shippingPath = createConstantVariable("shippingPath", "/shipping");
        this.addVariables(server, port,cartserver,cartsport,basicPath,shippingPath);
    }

    @java.lang.Override
    public void createServers() {
        final Variable server = getVariableByName("host");
        final Variable port = getVariableByName("port");
        final Variable cartsserver = getVariableByName("carts_host");
        final Variable cartsport = getVariableByName("carts_port");
        this.addServer(createServer("Server", server, port));
        this.addServer(createServer("Carts_Server", cartsserver, cartsport));
    }
}
