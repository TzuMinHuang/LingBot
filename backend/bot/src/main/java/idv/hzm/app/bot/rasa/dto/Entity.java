package idv.hzm.app.bot.rasa.dto;

public class Entity {
    private String entity;
    private String value;
    private int start;
    private int end;
    private String role;
    private String group;
	public String getEntity() {
		return entity;
	}
	public void setEntity(String entity) {
		this.entity = entity;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	@Override
	public String toString() {
		return "Entity [entity=" + entity + ", value=" + value + ", start=" + start + ", end=" + end + ", role=" + role
				+ ", group=" + group + "]";
	}

    // getters and setters
    
}

