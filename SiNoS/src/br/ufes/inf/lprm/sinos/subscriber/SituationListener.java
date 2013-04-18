package br.ufes.inf.lprm.sinos.subscriber;

import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.lang.reflect.ParameterizedType;

import org.drools.runtime.StatefulKnowledgeSession;

import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.situation.SituationType;

/**
 * 
 * This class provides methods for handling situation notifications from the subscribed channel.
 *
 * @param <T> The type of the situation this listener will handle. This class must extend the org.drools.situation.base.SituationType class.
 */
public abstract class SituationListener <T extends SituationType> {

	private StatefulKnowledgeSession ksession;
	private String channelName;
	
	/**
	 * Initializes the Situation Listener.
	 */
	public SituationListener() {
		ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
		
		ObjectStreamClass osc = ObjectStreamClass.lookup((Class<?>) pt.getActualTypeArguments()[0]);
		String cn = osc.getName() + "&%&";
		for(ObjectStreamField field : osc.getFields()){
			cn += field.getName();
			cn += field.getTypeString();
			cn += field.getTypeCode();
		}
		
		this.channelName = cn;
	}
	
	/**
	 * Initializes the Situation Listener and enables the mirroring functionality.
	 * @param ksession The Stateful Knowledge Session which will receive situations by means of the mirroring functionality.
	 */
	public SituationListener (StatefulKnowledgeSession ksession){
		this();
		this.ksession = ksession;
	}
	
	/**
	 * This method is invoked when the connected situation channel has been closed for some reason.
	 * @param reason The reason why the channel has been closed.
	 */
	public abstract void onDisconnection (DisconnectionReason reason);

	/**
	 * This method is invoked when a situation activation happens.
	 * For performance reasons, you should implement this method only if this type of event is of your application's interest.
	 * @param situation The situation which was activated.
	 */
	public void onSituationActivation (T situation) {}
	
	/**
	 * This method is invoked when a situation deactivation happens.
	 * For performance reasons, you should implement this method only if this type of event is of your application's interest.
	 * @param situation The situation which was deactivated.
	 */
	public void onSituationDeactivation (T situation) {}
	
	/**
	 * Retrieves the name of the situation channel to which this listener is subscribed.
	 * @return The event channel's name. 
	 */
	final public String getChannelName () {
		return channelName;
	}
	
	/**
	 * Retrieves the mirrored Stateful Knowledge Session.
	 * @return The Stateful Knowledge Session if the mirroring feature is enabled, null otherwise.
	 */
	final public StatefulKnowledgeSession getStatefulKnowledgeSession () {
		return ksession;
	}
	
	/**
	 * Indicates whether the mirroring functionality is enabled or not.
	 * @return true if the mirroring feature is enabled, false otherwise.
	 */
	final public boolean isMirrored () {
		return ksession == null ? false : true;
	}
}
