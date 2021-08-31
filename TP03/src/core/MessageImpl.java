package core;

public class MessageImpl implements Message{

	private static final long serialVersionUID = -1298874117877687170L;
	private String type;
	private int id;
	private String content;
	private int brokerId;
		
	public MessageImpl(){
		id = -1;
		type = null;
		content = null;
	}
	
	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public void setType(String type) {
		this.type = type;		
	}

	@Override
	public int getLogId() {
		return id;
	}

	@Override
	public void setLogId(int id) {
		this.id = id;		
	}

	@Override
	public String getContent() {
		return content;
	}

	@Override
	public void setContent(String content) {
		this.content = content;		
	}

	@Override
	public int getBrokerId() {
		return brokerId;
	}

	@Override
	public void setBrokerId(int id) {
		brokerId = id;
		
	}
	
}
