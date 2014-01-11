package br.ufes.inf.lprm.sinos.subscriber;

import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.lang.reflect.ParameterizedType;

import org.drools.runtime.StatefulKnowledgeSession;

import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.situation.SituationType;

/**
 * 
 * This class provides methods for handling situation notifications.
 *
 * @param <T> The situation type class that this listener will handle. This class must extend the org.drools.situation.base.SituationType class.
 */
public abstract class SituationListener <T extends SituationType> {

	private StatefulKnowledgeSession ksession;
	private String channelName;
	private String channelId;
	
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

		this.channelId = cn;
		this.channelName = osc.getName();
	}
	
	/**
	 * Initializes the Situation Listener and enables mirroring functionality.
	 * For performance reasons, you should enable mirroring functionality only if it is of your application's interest.
	 * @param ksession The Stateful Knowledge Session to be mirrored.
	 */
	public SituationListener (StatefulKnowledgeSession ksession){
		this();
		this.ksession = ksession;
	}
	
	/**
	 * This method is invoked on the closure of the connected situation channel.
	 * @param reason The reason why the channel has been closed.
	 */
	public abstract void onDisconnection (DisconnectionReason reason);

	/**
	 * This method is invoked when a situation activation happens.
	 * For performance reasons, you should implement this method only if this type of notification is of your application's interest.
	 * @param situation The situation which was activated.
	 */
	public void onSituationActivation (T situation) {}
	
	/**
	 * This method is invoked when a situation deactivation happens.
	 * For performance reasons, you should implement this method only if this type of notification is of your application's interest.
	 * @param situation The situation which was deactivated.
	 */
	public void onSituationDeactivation (T situation) {}
	
	/**
	 * Retrieves the id of the situation channel to which this listener is subscribed.
	 * @return The situation channel's id. 
	 */
	final public String getChannelId () {
		return channelId;
	}
	
	/**
	 * Retrieves the name of the situation channel to which this listener is subscribed.
	 * @return The situation channel's name. 
	 */
	final public String getChannelName () {
		return channelName;
	}
	
	/**
	 * Retrieves the mirrored Stateful Knowledge Session.
	 * @return The Stateful Knowledge Session if mirroring functionality is enabled, null otherwise.
	 */
	final public StatefulKnowledgeSession getStatefulKnowledgeSession () {
		return ksession;
	}
	
	/**
	 * Indicates whether mirroring functionality is enabled or not.
	 * @return true if mirroring functionality is enabled, false otherwise.
	 */
	final public boolean isMirrored () {
		return ksession == null ? false : true;
	}
}
