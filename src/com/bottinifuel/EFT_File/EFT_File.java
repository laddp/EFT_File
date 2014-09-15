/**
 * 
 */
package com.bottinifuel.EFT_File;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.bottinifuel.EFT_File.Records.BatchControlTotal;
import com.bottinifuel.EFT_File.Records.BatchHeader;
import com.bottinifuel.EFT_File.Records.EFT_Record;
import com.bottinifuel.EFT_File.Records.EFT_Record.RecordTypeEnum;
import com.bottinifuel.EFT_File.Records.EntryDetail;
import com.bottinifuel.EFT_File.Records.EntryDetailAddenda;
import com.bottinifuel.EFT_File.Records.FileControl;
import com.bottinifuel.EFT_File.Records.FileHeader;

/**
 * @author Patrick Ladd
 *
 */
public class EFT_File {
	public List<BatchHeader> Batches = new LinkedList<BatchHeader>();
	public FileHeader Header = null;

	private List<EFT_Record>  Records = new LinkedList<EFT_Record>();

    public EFT_File(InputStream is, boolean allowInvalidABA) throws Exception
    {
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(is));

        BatchHeader batch      = null;
		EFT_Record  prevRecord = null;
        
        while (reader.ready())
        	try {
        		String inputLine = " " + reader.readLine();
                if (inputLine.length() != 95)
                	throw new ParseException("Invalid record length: Line " + reader.getLineNumber() +
                			" - got " + (inputLine.length() - 1) + " expected 94", 1);
        		RecordTypeEnum type = RecordTypeEnum.recordType(inputLine.charAt(1));
        		EFT_Record record;
        		
        		if (Header != null && Header.EndFile != null)
        		{
        			if (type != EFT_Record.RecordTypeEnum.FILE_CONTROL)
        				throw new ParseException("Data record found after end-of-file, line #" + reader.getLineNumber(), 0);
        			if (!inputLine.matches(" 9{94}"))
        				throw new ParseException("Invalid pad record, line #" + reader.getLineNumber(), 0);
        			continue;
        		}
        		switch (type)
        		{
        		case FILE_HEADER:
					if (Header != null)
						throw new ParseException("FILE_HEADER at line " + reader.getLineNumber() +
								" with already active header, line #" + Header.RecordNum, 1);
        			record = Header = new FileHeader(reader.getLineNumber(), inputLine);
        			break;
        		case BATCH_HEADER:
        			if (Header == null)
        				throw new ParseException("BATCH_HEADER with no active FILE_HEADER, line " + reader.getLineNumber(), 1);
        			if (batch != null)
        				throw new ParseException("BATCH_HEADER at line " + reader.getLineNumber() +
        						" with already active header, line #" + batch.RecordNum, 1);
        			record = batch = new BatchHeader(reader.getLineNumber(), inputLine, Header);
        			Header.Batches.add(batch);
        			Batches.add(batch);
        			break;
        		case ENTRY_DETAIL:
        			if (Header == null)
        				throw new Exception("ENTRY_DETAIL with no active FILE_HEADER, line " + reader.getLineNumber());
        			if (batch == null)
        				throw new Exception("ENTRY_DETAIL with no active BATCH_HEADER, line " + reader.getLineNumber());
        			EntryDetail entry = new EntryDetail(reader.getLineNumber(), inputLine, batch, allowInvalidABA);
        			batch.Entries.add(entry);
        			record = entry;
        			break;
        		case ENTRY_DETAIL_ADDENDA:
        			if (Header == null)
        				throw new Exception("ENTRY_DETAIL_ADDENDA with no active FILE_HEADER, line " + reader.getLineNumber());
        			if (batch == null)
        				throw new Exception("ENTRY_DETAIL_ADDENDA with no active BATCH_HEADER, line " + reader.getLineNumber());
        			if (prevRecord.RecordType != EFT_Record.RecordTypeEnum.ENTRY_DETAIL)
        				throw new Exception("ENTRY_DETAIL_ADDENDA doesn't follow ENTRY_DETAIL, line " + reader.getLineNumber());
        			EntryDetailAddenda entryAddenda = new EntryDetailAddenda(reader.getLineNumber(), inputLine);
        			((EntryDetail)prevRecord).Addenda = entryAddenda;
        			batch.Addenda.add(entryAddenda);
        			record = entryAddenda;
        			break;
        		case BATCH_CONTROL_TOTAL:
        			if (batch == null)
        				throw new Exception("BATCH_CONTROL_TOTAL at line " + reader.getLineNumber() +
        						" with no active batch");
        			BatchControlTotal batchEnd = new BatchControlTotal(reader.getLineNumber(), inputLine, batch);
        			record = batch.BatchEnd = batchEnd;
        			batch = null;
        			break;
        		case FILE_CONTROL:
        			if (batch != null)
        				throw new Exception("FILE_CONTROL at line " + reader.getLineNumber() +
        						" with active batch from line " + batch.RecordNum);
        			if (Header == null)
        				throw new Exception("FILE_CONTROL at line " + reader.getLineNumber() +
        						" with no active file");
        			FileControl endFile = new FileControl(reader.getLineNumber(), inputLine, Header);
        			record = Header.EndFile = endFile;
        			break;
        		default:
        			throw new Exception("Unknown record type: " + type.RecordCode);
        		}
    			Records.add(record);
    			prevRecord = record;
        	}
        	catch (IOException e)
        	{
        		throw new Exception("Line #" + reader.getLineNumber() + ": I/O exception: " + e);
        	}
    }

    
    public void WriteFile(OutputStream o) throws IOException
    {
    	PrintStream out = new PrintStream(o);
    	int lineCount = 0;
    	for (EFT_Record record : Records)
    	{
    		lineCount++;
    		if (record != Header.EndFile)
    			out.println(record.dump());
    	}
    	
    	int newBlockCount = calculateBlockCount();
    	Header.EndFile.setBlockCount(newBlockCount);
    	out.println(Header.EndFile.dump());

    	int linesToAdd = (Header.BlockingFactor * newBlockCount) - lineCount;
    	if (linesToAdd != 0)
    	{
        	String fillerLine = "";
        	for (int i = 0; i < Header.RecordSize; i++)
        		fillerLine += '9';

        	for (int i = 0; i < linesToAdd; i++)
    			out.println(fillerLine);
    	}
    }
    
    public Date getBatchDate()
    {
    	return Batches.get(0).getEffectiveEntryDate();
    }
    
    public void setBatchDates(Date newDate)
    {
    	for (BatchHeader batch : Batches)
    		batch.setEffectiveEntryDate(newDate);
    }
    
    
    public int calculateBlockCount()
    {
    	int recordCount = Records.size();
    	int blocks = recordCount / Header.BlockingFactor;
    	if (recordCount % Header.BlockingFactor != 0)
    		blocks += 1;
    	return blocks;
    }
    
    public void RemoveTransaction(EntryDetail trans) throws Exception
    {
    	BatchHeader batch = trans.Batch;

    	if (batch.Entries.size() == 1)
    		throw new Exception("Can't handle removing last transaction in a batch");
    	
    	// TODO: THIS DOES NOT HANDLE DETAIL ADDENDA
    	if (batch.Addenda.size() != 0)
    		System.out.println("WARNING: REMOVING TRANSACTION FROM A FILE WITH ADDENDA - NOT IMPLEMENTED");

    	Records.remove(trans);
    	batch.RemoveTransaction(trans);
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			EFT_File eft;
			eft = new EFT_File(new FileInputStream("//bottinibeta/shared/IT Department/Programming/EFT/eftSample.nacha"), false);
			eft = new EFT_File(new FileInputStream("//bottinibeta/shared/IT Department/Destwin/echecks_log_20110420_csr.txt"), false);
			eft = new EFT_File(new FileInputStream("//bottinibeta/shared/IT Department/Destwin/echecks_log_20110510_csr.txt"), false);
			eft = new EFT_File(new FileInputStream("//bottinibeta/shared/IT Department/Destwin/echecks_log_20110511_csr.txt"), false);
			eft = new EFT_File(new FileInputStream("//bottinibeta/shared/IT Department/Programming/EFT//EFT050211172007_3-JP Morgan ChaseCCD.nacha"), false);
			eft = new EFT_File(new FileInputStream("//bottinibeta/shared/IT Department/Programming/EFT//EFT051011132706_3-JP Morgan ChaseCCD.nacha"), false);
			eft.toString();
			return;
		}
		catch (Exception e)
		{
			System.out.println(e);
			return;
		}
	}
}

