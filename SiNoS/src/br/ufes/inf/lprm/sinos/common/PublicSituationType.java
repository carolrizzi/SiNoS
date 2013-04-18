package br.ufes.inf.lprm.sinos.common;

import java.rmi.server.UID;

import br.ufes.inf.lprm.situation.SituationType;

public class PublicSituationType extends SituationType {

	private static final long serialVersionUID = 1L;
	private UID uid = new UID();
	private int hashcode;
	
	public PublicSituationType() {
		hashcode = 31 * 1 + ((uid == null) ? 0 : uid.hashCode());
	}
	
	@Override
	public int hashCode() {
		return hashcode;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PublicSituationType other = (PublicSituationType) obj;
		if (hashcode != other.hashCode())
			return false;
		return true;
	}

}
