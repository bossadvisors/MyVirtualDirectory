/*******************************************************************************
 * Copyright 2016, 2017 Tremolo Security, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package net.sourceforge.myvd.inserts.jdbc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;








import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPModification;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.util.RDN;

import net.sourceforge.myvd.chain.AddInterceptorChain;
import net.sourceforge.myvd.chain.BindInterceptorChain;
import net.sourceforge.myvd.chain.CompareInterceptorChain;
import net.sourceforge.myvd.chain.DeleteInterceptorChain;
import net.sourceforge.myvd.chain.ExetendedOperationInterceptorChain;
import net.sourceforge.myvd.chain.ModifyInterceptorChain;
import net.sourceforge.myvd.chain.PostSearchCompleteInterceptorChain;
import net.sourceforge.myvd.chain.PostSearchEntryInterceptorChain;
import net.sourceforge.myvd.chain.RenameInterceptorChain;
import net.sourceforge.myvd.chain.SearchInterceptorChain;
import net.sourceforge.myvd.core.NameSpace;
import net.sourceforge.myvd.inserts.Insert;
import net.sourceforge.myvd.types.Attribute;
import net.sourceforge.myvd.types.Bool;
import net.sourceforge.myvd.types.DistinguishedName;
import net.sourceforge.myvd.types.Entry;
import net.sourceforge.myvd.types.ExtendedOperation;
import net.sourceforge.myvd.types.Filter;
import net.sourceforge.myvd.types.Int;
import net.sourceforge.myvd.types.Password;
import net.sourceforge.myvd.types.Results;
import net.sourceforge.myvd.util.PBKDF2;

public class Pbkdf2Auth implements Insert {
	static Logger log = org.apache.logging.log4j.LogManager.getLogger(Pbkdf2Auth.class.getName());
	private String name;
	private NameSpace ns;
	private JdbcPool jdbc;
	
	private String sql;

	
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void configure(String name, Properties props, NameSpace nameSpace)
			throws LDAPException {
		this.name = name;
		this.ns = nameSpace;
		this.jdbc = null;
		
		this.sql = props.getProperty("sql");
		
		log.info("SQL : '" + sql + "'");
		
		
		
		
	}

	@Override
	public void add(AddInterceptorChain chain, Entry entry,
			LDAPConstraints constraints) throws LDAPException {
		chain.nextAdd(entry, constraints);
		
	}

	@Override
	public void bind(BindInterceptorChain chain, DistinguishedName dn,
			Password pwd, LDAPConstraints constraints) throws LDAPException {
		if (this.jdbc == null) {
			this.findJdbcInsert();
		}
		
		
		Connection con  = null;
		boolean success = false;
		
		
		try {
			con = this.jdbc.getCon();
			
			RDN rdn = (RDN) dn.getDN().getRDNs().get(0);
			if (log.isDebugEnabled()) {
				log.debug("User RDN : '" + rdn.getValue() + "'");
				
			}
			
			PreparedStatement ps = con.prepareStatement(this.sql);
			ps.setString(1, rdn.getValue());
			
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String hashedPassword = rs.getString(1);
				success = PBKDF2.checkPassword(new String(pwd.getValue()), hashedPassword);
			} else {
				success = false;
			}
			
			rs.close();
			ps.close();
			
		} catch (Throwable t) {
			throw new LDAPException("Could not execute bind",LDAPException.OPERATIONS_ERROR,LDAPException.resultCodeToString(LDAPException.OPERATIONS_ERROR),t);
		} finally {
			if (con != null) {
				this.jdbc.returnCon(con);
			}
		}
		
		if (! success) {
			throw new LDAPException(LDAPException.resultCodeToString(LDAPException.INVALID_CREDENTIALS),LDAPException.INVALID_CREDENTIALS,LDAPException.resultCodeToString(LDAPException.INVALID_CREDENTIALS));
		}
		
		
	}

	@Override
	public void compare(CompareInterceptorChain chain, DistinguishedName dn,
			Attribute attrib, LDAPConstraints constraints) throws LDAPException {
		chain.nextCompare(dn, attrib, constraints);
		
	}

	@Override
	public void delete(DeleteInterceptorChain chain, DistinguishedName dn,
			LDAPConstraints constraints) throws LDAPException {
		chain.nextDelete(dn, constraints);
		
	}

	@Override
	public void extendedOperation(ExetendedOperationInterceptorChain chain,
			ExtendedOperation op, LDAPConstraints constraints)
			throws LDAPException {
		chain.nextExtendedOperations(op, constraints);
		
	}

	@Override
	public void modify(ModifyInterceptorChain chain, DistinguishedName dn,
			ArrayList<LDAPModification> mods, LDAPConstraints constraints)
			throws LDAPException {
		chain.nextModify(dn, mods, constraints);
		
	}

	@Override
	public void search(SearchInterceptorChain chain, DistinguishedName base,
			Int scope, Filter filter, ArrayList<Attribute> attributes,
			Bool typesOnly, Results results, LDAPSearchConstraints constraints)
			throws LDAPException {
		chain.nextSearch(base, scope, filter, attributes, typesOnly, results, constraints);
		
	}

	@Override
	public void rename(RenameInterceptorChain chain, DistinguishedName dn,
			DistinguishedName newRdn, Bool deleteOldRdn,
			LDAPConstraints constraints) throws LDAPException {
		chain.nextRename(dn, newRdn, deleteOldRdn, constraints);
		
	}

	@Override
	public void rename(RenameInterceptorChain chain, DistinguishedName dn,
			DistinguishedName newRdn, DistinguishedName newParentDN,
			Bool deleteOldRdn, LDAPConstraints constraints)
			throws LDAPException {
		chain.nextRename(dn, newRdn, newParentDN, deleteOldRdn, constraints);
		
	}

	@Override
	public void postSearchEntry(PostSearchEntryInterceptorChain chain,
			Entry entry, DistinguishedName base, Int scope, Filter filter,
			ArrayList<Attribute> attributes, Bool typesOnly,
			LDAPSearchConstraints constraints) throws LDAPException {
		chain.nextPostSearchEntry(entry, base, scope, filter, attributes, typesOnly, constraints);
		
	}

	@Override
	public void postSearchComplete(PostSearchCompleteInterceptorChain chain,
			DistinguishedName base, Int scope, Filter filter,
			ArrayList<Attribute> attributes, Bool typesOnly,
			LDAPSearchConstraints constraints) throws LDAPException {
		chain.nextPostSearchComplete(base, scope, filter, attributes, typesOnly, constraints);
		
	}

	@Override
	public void shutdown() {
		
		
	}
	
	private synchronized void findJdbcInsert() throws LDAPException {
		if (this.jdbc != null) {
			return;
		}
		
		for (int i=0;i<this.ns.getChain().getLength();i++) {
			if (this.ns.getChain().getInsert(i) instanceof JdbcPool) {
				this.jdbc = (JdbcPool) this.ns.getChain().getInsert(i);
				break;
			}
		}
		
		if (this.jdbc == null) {
			throw new LDAPException("No Jdbc Insert found on this chain",LDAPException.OPERATIONS_ERROR,LDAPException.resultCodeToString(LDAPException.OPERATIONS_ERROR));
		}
	}

}
