/**
 * 
 */
package com.bottinifuel.EFT_File.Records;

/**
 * @author adminpat
 *
 */
public class EntryDetailAddenda extends EFT_Record {
	public EntryDetailAddenda(int recnum, String data)
    {
		super(RecordTypeEnum.ENTRY_DETAIL_ADDENDA, recnum, data);
    }
}
