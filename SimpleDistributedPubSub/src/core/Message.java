/**
 * 
 */
package core;

import java.io.Serializable;


public interface Message extends Serializable{
	
	public abstract String getType();
	public abstract void setType(String type);
	public abstract int getLogId();
	public abstract void setLogId(int id);
	public abstract String getContent();
	public abstract void setContent(String content);
	public abstract int getBrokerId();
	public abstract void setBrokerId(int id);
		
}
