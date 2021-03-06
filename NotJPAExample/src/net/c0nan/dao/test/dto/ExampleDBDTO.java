package net.c0nan.dao.test.dto;

import net.c0nan.dao.annotations.AuditField;
import net.c0nan.dao.annotations.AuditField.AuditFieldType;
import net.c0nan.dao.annotations.Bindable;
import net.c0nan.dao.annotations.DBField;
import net.c0nan.dao.annotations.DBKey;
import net.c0nan.dao.annotations.NamedQueries;
import net.c0nan.dao.annotations.NamedQuery;
import net.c0nan.dao.annotations.TemporalType;
import net.c0nan.dao.annotations.TemporalType.TemporalTypes;
import net.c0nan.dao.dto.BaseDBDTO;
import net.c0nan.dao.test.connection.ExampleConnectionManager;
@NamedQueries(Queries={
	@NamedQuery(Name="createDB",Query="CREATE TABLE TEST1 (ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), CODE CHAR(10) , DESCRIPTION CHAR(150),AUDTIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, PRIMARY KEY (ID))"),
	@NamedQuery(Name="deleteDB",Query="DROP TABLE TEST1")
})
@Bindable(ConnectionManager=ExampleConnectionManager.class,Table="TEST1")
public class ExampleDBDTO extends BaseDBDTO{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@DBKey(AutoGenerated=true)
	@DBField(MapTo = "ID") 
	private Long id;
	
	@DBField(MapTo = "CODE")
	private String code;

	@DBField(MapTo = "DESCRIPTION")
	private String description;
	
	@DBField(MapTo = "AUDTIME")
	@AuditField(Type=AuditFieldType.TIMESTAMP)
	@TemporalType(Type=TemporalTypes.TIMESTAMP)
    private String auditTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAuditTime() {
		return auditTime;
	}

	public void setAuditTime(String auditTime) {
		this.auditTime = auditTime;
	}

	

}
