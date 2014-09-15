/**
 * 
 */
package com.bottinifuel.EFT_File.Records;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Patrick Ladd
 *
 */
public class BatchHeader extends EFT_Record {
	public List<EntryDetail> Entries = new LinkedList<EntryDetail>();
	public List<EntryDetailAddenda> Addenda = new LinkedList<EntryDetailAddenda>();
	public BatchControlTotal BatchEnd = null;

	public final int        ServiceClassCode;
	public final String     CompanyName;
	public final String     CompanyDiscretionaryData;
	public final BigInteger CompanyID;
	public final String     EntryClassCode;
	public final String     CompanyEntryDescription;
	public final Date		CompanyDescriptiveDate;
	private      Date		EffectiveEntryDate;
	public final char		OriginatorStatusCode;
	public final BigInteger OriginatorDFI;
	public final BigInteger BatchNumber;
	public final FileHeader File;
	
	private BigDecimal Credits   = null;
	private BigDecimal Debits    = null;
	private BigInteger Checksums = null;
	
	public BatchHeader(int recnum, String data, FileHeader file) throws java.text.ParseException
    {
		super(RecordTypeEnum.BATCH_HEADER, recnum, data);
		File = file;
		
		ServiceClassCode = Integer.valueOf(data.substring(2, 5));
		CompanyName              = data.substring( 5, 21).trim();
		CompanyDiscretionaryData = data.substring(21, 41).trim();
		CompanyID = new BigInteger(data.substring(41, 51));
		EntryClassCode           = data.substring(51, 54).trim();
		CompanyEntryDescription  = data.substring(54, 64).trim();
		
		DateFormat df = new SimpleDateFormat("yyMMdd");
		CompanyDescriptiveDate = df.parse(data.substring(64, 70));
		EffectiveEntryDate     = df.parse(data.substring(70, 76));
		
		String blanks = data.substring(76, 79);
		if (blanks.trim().length() != 0)
			throw new java.text.ParseException("Expected SettlementDate to be blank, line #" + recnum, 76);
		
		OriginatorStatusCode = data.charAt(79);
		OriginatorDFI = new BigInteger(data.substring(80, 88));
		BatchNumber   = new BigInteger(data.substring(88));
    }
	
	
	private void CalculateBatchTotals()
	{
		Checksums = new BigInteger("0");
		Debits    = new BigDecimal(0);
		Credits   = new BigDecimal(0);
		for (EntryDetail entry : Entries)
		{
			Checksums = Checksums.add(entry.ReceivingDFI.GetNumberNoChecksum());
			if (entry.IsDebit())
				Debits  = Debits. add(entry.Amount);
			else if (entry.IsCredit())
				Credits = Credits.add(entry.Amount);
		}
	}
	
	public BigDecimal TotalCredits()
	{
		if (Credits == null)
			CalculateBatchTotals();
		return Credits;
	}
	public BigDecimal TotalDebits()
	{
		if (Debits == null)
			CalculateBatchTotals();
		return Debits;
	}
	public BigInteger Checksum()
	{
		if (Checksums == null)
			CalculateBatchTotals();
		return Checksums;
	}

	public void RemoveTransaction(EntryDetail trans) throws Exception
	{
		// These need to be recomputed next time requested
		Credits   = null;
		Debits    = null;
		Checksums = null;
		
		if (Entries.remove(trans) != true)
			throw new Exception("Removing transaction that is not in batch");
		BatchEnd.removeTransaction(trans);
		File.EndFile.RemoveTransaction(trans);
		
		//TODO: DEBUG only
		BatchEnd.CheckBatch();
	}
	
	public String dump()
	{
		String rc = "5";
		rc += String.format("%03d%-16s%-20s", ServiceClassCode, CompanyName, CompanyDiscretionaryData);
		rc += String.format("%09d%-3s%-10s", CompanyID, EntryClassCode, CompanyEntryDescription); 
		rc += String.format("%ty%tm%td",
				CompanyDescriptiveDate, CompanyDescriptiveDate, CompanyDescriptiveDate);
		rc += String.format("%ty%tm%td   1",
				EffectiveEntryDate, EffectiveEntryDate, EffectiveEntryDate);
		rc += String.format("%08d%07d", OriginatorDFI, BatchNumber);
		if (rc.equals(Data))
			return Data;
		else
		{
//			System.out.println(Data);
//			System.out.println(rc);
			return rc;
		}
	}
	
	public Date getEffectiveEntryDate() {
		return EffectiveEntryDate;
	}


	public void setEffectiveEntryDate(Date effectiveEntryDate) {
		EffectiveEntryDate = effectiveEntryDate;
	}

}
