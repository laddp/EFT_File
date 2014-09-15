/**
 * 
 */
package com.bottinifuel.EFT_File.Records;


/**
 * @author Patrick Ladd
 *
 */
public abstract class EFT_Record {
    /**
	 * @author adminpat
	 *
	 */
	public enum RecordTypeEnum {
        FILE_HEADER         ('1'),
        BATCH_HEADER        ('5'),
        ENTRY_DETAIL        ('6'),
        ENTRY_DETAIL_ADDENDA('7'),
        BATCH_CONTROL_TOTAL ('8'),
        FILE_CONTROL        ('9');

        public final char RecordCode;
        
        RecordTypeEnum(char code)
        {
            RecordCode = code;
        }
        
        public static RecordTypeEnum recordType(char rt) throws Exception
        {
            for (RecordTypeEnum t : RecordTypeEnum.values())
                if (rt == t.RecordCode)
                    return t;
            throw new Exception("Invalid record type " + rt);
        }
    }

    public    final RecordTypeEnum RecordType;
    public    final int            RecordNum;
    protected final String         Data; 
    
    public EFT_Record(RecordTypeEnum rt, int recnum, String d)
    {
        RecordType = rt;
        RecordNum = recnum;
        Data = d.substring(1);
    }

    
    public String dump()
	{
		return Data;
	}
    
    
    public String toString()
    {
        return "Record #" + RecordNum + " - " + RecordType;
    }
}
