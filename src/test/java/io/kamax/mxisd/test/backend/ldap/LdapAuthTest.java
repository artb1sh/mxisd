/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.test.backend.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import io.kamax.mxisd.backend.ldap.LdapAuthProvider;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ldap.LdapConfig;
import io.kamax.mxisd.config.ldap.generic.GenericLdapConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;

public class LdapAuthTest {

    private static InMemoryDirectoryServer ds;
    private static ArrayList<String> dnList = new ArrayList<>();

    @BeforeClass
    public static void beforeClass() throws LDAPException {
        dnList.add("dc=1,dc=mxisd,dc=example,dc=org");
        dnList.add("dc=2,dc=mxisd,dc=example,dc=org");
        dnList.add("dc=3,dc=mxisd,dc=example,dc=org");

        InMemoryListenerConfig lCfg = InMemoryListenerConfig.createLDAPConfig("localhost", 65001);
        InMemoryDirectoryServerConfig config =
                new InMemoryDirectoryServerConfig(dnList.get(0), dnList.get(1), dnList.get(2));
        config.addAdditionalBindCredentials("cn=mxisd", "mxisd");
        config.setListenerConfigs(lCfg);

        ds = new InMemoryDirectoryServer(config);
        ds.startListening();
    }

    @AfterClass
    public static void afterClass() {
        ds.shutDown(true);
    }

    @Test
    public void singleDn() {
        MatrixConfig mxCfg = new MatrixConfig();
        mxCfg.setDomain("example.org");
        mxCfg.build();

        LdapConfig cfg = new GenericLdapConfig();
        cfg.getConnection().setHost("localhost");
        cfg.getConnection().setPort(65001);
        cfg.getConnection().setBaseDn(dnList.get(0));
        cfg.getConnection().setBindDn("cn=mxisd");
        cfg.getConnection().setBindPassword("mxisd");

        LdapConfig.UID uid = new LdapConfig.UID();
        uid.setType("uid");
        uid.setValue("saMAccountName");
        cfg.getAttribute().setUid(uid);
        cfg.build();

        LdapAuthProvider p = new LdapAuthProvider(cfg, mxCfg);
        BackendAuthResult result = p.authenticate(MatrixID.from("john", "example.org").valid(), "doe");
        assertFalse(result.isSuccess());
    }

    @Test
    public void multiDNs() {
        MatrixConfig mxCfg = new MatrixConfig();
        mxCfg.setDomain("example.org");
        mxCfg.build();

        LdapConfig cfg = new GenericLdapConfig();
        cfg.getConnection().setHost("localhost");
        cfg.getConnection().setPort(65001);
        cfg.getConnection().setBaseDNs(dnList);
        cfg.getConnection().setBindDn("cn=mxisd");
        cfg.getConnection().setBindPassword("mxisd");

        LdapConfig.UID uid = new LdapConfig.UID();
        uid.setType("uid");
        uid.setValue("saMAccountName");
        cfg.getAttribute().setUid(uid);
        cfg.build();

        LdapAuthProvider p = new LdapAuthProvider(cfg, mxCfg);
        BackendAuthResult result = p.authenticate(MatrixID.from("john", "example.org").valid(), "doe");
        assertFalse(result.isSuccess());
    }

}
