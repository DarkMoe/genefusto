package gen;

public class GenVdp {

	int[] vram  = new int[0x10000];
	int[] cram  = new int[0x80];		//	The CRAM contains 128 bytes, addresses 0 to 7F
	int[] vsram = new int[0x50];		//	The VSRAM contains 80 bytes, addresses 0 to 4F
	
	int[] registers = new int[24];
	int addr = 0x230;
	
	//	FIFO Buffer 4 levels
	int[] fifoCode = new int[4];
	int[] fifoAddress = new int[4];
	int[] fifoData = new int[4];
	
	int currentFIFOReadEntry;
	int currentFIFOWriteEntry;
	
	int nextFIFOReadEntry;
	int nextFIFOWriteEntry;
	
	boolean controlSecond;
	
	boolean addressSecondWrite = false;
	long firstWrite;
	
	int dataPort;
	int addressPort;
	
	//	Reg 0
	//	Vertical Scroll Inhibit
	boolean vsi;
	//	Horizontal Scroll Inhibit
	boolean hsi;
	//	Left Column Blank
	boolean lcb;
	//	Enable HINT
	boolean ie1;
	//	Sprite Shift (mode 4) / HSync Mode (mode 5)
	boolean ssHsm;
	//	Palette Select
	boolean ps;
	//	HV Counter Latch
	boolean m2;
	//	External Sync
	boolean es;
	
	//	REG 1
	//	Extended VRAM
	boolean evram;
	//	Enable Display
	boolean disp;
	//	Enable VINT
	boolean ie0;
	//	Enable DMA
	boolean m1;
	//	Enable V30 Mode
	boolean m3;
	//	Enable Mode 5	(si esta inactivo, es mode = 4, compatibilidad con SMS)
	boolean m5;
	//	Sprite Size
	boolean sz;
	//	Sprite Zoom
	boolean mag;
	
	//	REG 0xF
	int autoIncrementData;
	
	//	reg 0x13
	int dmaLengthCounterLo;
	
	//	reg 0x14
	int dmaLengthCounterHi;
	
	//	reg 0x15
	int dmaSourceAddressLow;

	//	reg 0x16
	int dmaSourceAddressMid;

	//	reg 0x17
	int dmaSourceAddressHi;
	int dmaMode;
	
	boolean vramFill = false;
	
	//	Status register:
//	15	14	13	12	11	10	9		8			7	6		5		4	3	2	1	0
//	0	0	1	1	0	1	EMPTY	FULL		VIP	SOVR	SCOL	ODD	VB	HB	DMA	PAL
	
//	EMPTY and FULL indicate the status of the FIFO.
//	When EMPTY is set, the FIFO is empty.
//	When FULL is set, the FIFO is full.
//	If the FIFO has items but is not full, both EMPTY and FULL will be clear.
//	The FIFO can hold 4 16-bit words for the VDP to process. If the M68K attempts to write another word once the FIFO has become full, it will be frozen until the first word can be delivered.
	int empty;
	int full;
	
//	VIP indicates that a vertical interrupt has occurred, approximately at line $E0. It seems to be cleared at the end of the frame.
	int vip;
	
//	SOVR is set when there are too many sprites on the current scanline. The 17th sprite in 32 cell mode and the 21st sprite on one scanline in 40 cell mode will cause this.
	int sovr;
//	SCOL is set when any sprites have non-transparent pixels overlapping. This is cleared when the Control Port is read.
	int scol;
	
//	ODD is set if the VDP is currently showing an odd-numbered frame while Interlaced Mode is enabled.
	int odd;
//	VB returns the real-time status of the V-Blank signal. It is presumably set on line $E0 and unset at $FF.
	int vb;
//	HB returns the real-time status of the H-Blank signal.
	int hb;
//	DMA is set for the duration of a DMA operation. This is only useful for fills and copies, since the M68K is frozen during M68K to VRAM transfers.
	int dma;
//	PAL seems to be set when the system's display is PAL, and possibly reflects the state of having 240 line display enabled. The same information can be obtained from the version register.
	int pal;
	
	boolean vramWrite;
	boolean cramWrite;
	boolean vsramWrite;
	
	long all;
	
	int readControl() {
//		return (0x7000 		// Exodus prende bit 14-13-12
		return (
				(empty << 9)
				| (full << 8)
				| (vip << 7)
				| (sovr << 6)
				| (scol << 5)
				| (odd << 4)
				| (vb << 3)
				| (hb << 2)
				| (dma << 1)
				| (pal << 0)
				);
	}
	
	void init() {
		empty = 1;
		vb = 1;
		
		for (int i = 0; i < cram.length; i++) {
			if (i % 2 == 0) {
				cram[i] = 0x0E;
			} else {
				cram[i] = 0xEE;
			}
		}
		for (int i = 0; i < vsram.length; i++) {
			if (i % 2 == 0) {
				vsram[i] = 0x07;
			} else {
				vsram[i] = 0xFF;
			}
		}
	}

	//	https://wiki.megadrive.org/index.php?title=VDP_Ports#Write_2_-_Setting_RAM_address
//	First word
//	Bit	15	14	13	12	11	10	9	8	7	6	5	4	3	2	1	0
//	Def	CD1-CD0	A13		-										   A0

//	Second word
//	Bit	15	14	13	12	11	10	9	8	7	6	5	4	3	2	1	0
//	Def	0	 0	 0	 0	 0	 0	0	0	CD5		- CD2	0	A15	 -A14

//	Access mode	CD5	CD4	CD3	CD2	CD1	CD0
//	VRAM Write	0	0	0	0	0	1
//	CRAM Write	0	0	0	0	1	1
//	VSRAM Write	0	0	0	1	0	1
//	VRAM Read	0	0	0	0	0	0
//	CRAM Read	0	0	1	0	0	0
//	VSRAM Read	0	0	0	1	0	0
	
//	DMA Mode		CD5	CD4
//	Memory to VRAM	1	0
//	VRAM Fill		1	0
//	VRAM Copy		1	1
	public void writeControlPort(long data) {
		long mode = (data >> 13);
		
		if (!addressSecondWrite && mode == 0b100) {		//	Write 1 - Setting Register
			int dataControl = (int) (data & 0x00FF);
			int reg = (int) ((data >> 8) & 0x1F);
			
			System.out.println("REG: " + pad(reg) + " - data: " + pad(dataControl));
			
			registers[reg] = dataControl;
			
			if (reg == 0x00) {
				vsi = 	((data >> 7) & 1) == 1;
				hsi = 	((data >> 6) & 1) == 1;
				lcb = 	((data >> 5) & 1) == 1;
				ie1 = 	((data >> 4) & 1) == 1;
				ssHsm = ((data >> 3) & 1) == 1;
				ps = 	((data >> 2) & 1) == 1;
				m2 = 	((data >> 1) & 1) == 1;
				es = 	((data >> 0) & 1) == 1;
				
			} else if (reg == 0x01) {
				evram = ((data >> 7) & 1) == 1;
				disp = 	((data >> 6) & 1) == 1;
				ie0 = 	((data >> 5) & 1) == 1;
				m1 = 	((data >> 4) & 1) == 1;
				m3 = 	((data >> 3) & 1) == 1;
				m5 = 	((data >> 2) & 1) == 1;
				sz = 	((data >> 1) & 1) == 1;
				mag = 	((data >> 0) & 1) == 1;
			
			} else if (reg == 0x0F) {
				autoIncrementData = (int) (data & 0xFF);
			
			} else if (reg == 0x13) {
				dmaLengthCounterLo = (int) (data & 0xFF);
			
			} else if (reg == 0x14) {
				dmaLengthCounterHi = (int) (data & 0xFF);
			
			} else if (reg == 0x15) {
				dmaSourceAddressLow = (int) (data & 0xFF);
				
			} else if (reg == 0x16) {
				dmaSourceAddressMid = (int) (data & 0xFF);
			
			} else if (reg == 0x17) {
				dmaSourceAddressHi = (int) (data & 0x3F);
				dmaMode = (int) ((data >> 6) & 0x3);
			}
			
		} else { // Write 2 - Setting RAM address
			if (!addressSecondWrite) {
				firstWrite = data;
				addressSecondWrite = true;
				
			} else {
				addressSecondWrite = false;

				long first = firstWrite;
				long second = data;
				all = (first << 16) | second;
				
				int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
				int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 13));
				
				addressPort = addr;
				autoIncrementTotal = 0;	// reset este acumulador
				
				//	https://wiki.megadrive.org/index.php?title=VDP_DMA
				if ((code & 0b100000) > 0) { // DMA
					int dmaBits = code >> 4;
				
					dmaRecien = true;
				
					if 		  ((dmaBits & 0b01) > 0) {		//	Memory to VRAM	FIXME , no es claro como son los bits de acceso a este modo
						throw new RuntimeException("DMA WRITE !");
						
					} else if ((dmaBits & 0b10) > 0) {		//	VRAM Fill
//						FILL mode fills with same data from free even VRAM address.
//						FILL for only VRAM.
						
						vramFill = true;
						
					} else if ((dmaBits & 0b11) > 0) {		//	VRAM Copy
						throw new RuntimeException("DMA WRITE !");
						
					}
					
				} else {
					if 		  (code == 0b000000) {	//  VRAM Read
						throw new RuntimeException("ADDR WRITE !");
					
					} else if (code == 0b000001) {	//	VRAM Write
						vramWrite = true;
						cramWrite = false;
						vsramWrite = false;
					
					} else if (code == 0b001000) {	//	CRAM Read
						throw new RuntimeException("ADDR WRITE !");
					
					} else if (code == 0b000011) {	//	CRAM Write
						cramWrite = true;
						vramWrite = false;
						vsramWrite = false;
						
					} else if (code == 0b000100) {	//	VSRAM Read
						throw new RuntimeException("ADDR WRITE !");
					
					} else if (code == 0b000101) {	//	VSRAM Write
						vsramWrite = true;
						vramWrite = false;
						cramWrite = false;
					
					}
				}
			}
		}
	}
	
	boolean dmaRecien = false;
	
	public void dma() {
		if (dma == 1) {
			int dmaLength = (dmaLengthCounterHi << 8) | dmaLengthCounterLo;
			
			int index, data, destAddr;
			if (dmaRecien) {
				currentFIFOReadEntry = nextFIFOReadEntry;
				currentFIFOWriteEntry = nextFIFOWriteEntry;
				index = currentFIFOReadEntry;
				data = dataPort;
				destAddr = addressPort;
				
//				int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
				
				fifoData[index] = data;
//				fifoCode[index] = code;		// TODO implementar el code como esta en los otros writes de memory
			} else {
				index = currentFIFOReadEntry;
				data = fifoData[index];
				destAddr = fifoAddress[index];
			}
			
			System.out.println(pad4(dmaLength));
			
			vram[destAddr] = data >> 8;
			vram[destAddr + 1] = data & 0xFF;
			
			dmaLength = (dmaLength - 2);	// idem FIXME no es fijo
			if (dmaLength <= 0) {
				dma = 0;
				return;
			}
			
			dmaLength = dmaLength & 0xFFFF;
			dmaLengthCounterHi = dmaLength >> 8;
			dmaLengthCounterLo = dmaLength & 0xFF;
			
			addressPort += 2;
			fifoAddress[index] = destAddr + 2;	//	FIXME, no es fijo, se actualiza en paralelo mientras se siguen ejecutando instrucciones, hay q contar ciclos de cpu
			
			if (dmaRecien) {
				dmaRecien = false;
				
				index = (index + 1) % 4;
				nextFIFOReadEntry = index;
				nextFIFOWriteEntry = index;
			}
		}
	}

	boolean vramWrite2 = false;
	boolean cramWrite2 = false;
	boolean vsramWrite2 = false;
	
	int vramWriteData;
	int cramWriteData;
	int vsramWriteData;
	int firstData;
	
	public void writeDataPort(int data) {
		this.dataPort = data;

		if (vramFill) {
			if (m1) {
				dma = 1;
				vramFill = false;
				
				return;
			} else {
				System.out.println("M1 should be 1 in the DMA transfer. otherwise we can't guarantee the operation.");
			}
			
		} else if (vramWrite) {
			vramWrite(data);
			
		} else if (cramWrite) {
			cramWrite(data);
			
		} else if (vsramWrite) {
			vsramWrite(data);
			
		} else {
			throw new RuntimeException("NOT IMPL DMA !");
		}
	}

	int autoIncrementTotal;
	
	private void vramWrite(int data) {
		int word = data;
		
		int index = nextFIFOReadEntry;
		int address = addressPort;
		
		long first =  all >> 16;
		long second = all & 0xFFFF;
		
		int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 14));
		
		int offset = addr + autoIncrementTotal;
		
		int data1 = (word >> 8) & 0xFF;
		int data2 = (word >> 0) & 0xFF;
		
		vram[offset] 	 = data1;
		vram[offset + 1] = data2;
		fifoAddress[index] = offset;
		fifoCode[index] = code;
		fifoData[index] = word;
		
		int incrementOffset = autoIncrementTotal + autoIncrementData;

		address = address + incrementOffset;	// FIXME wrap
		offset = offset + incrementOffset;
		index = (index + 1) % 4;
		
		nextFIFOReadEntry = index;
		nextFIFOWriteEntry = index;
		autoIncrementTotal = incrementOffset;
	}
	
//	https://emu-docs.org/Genesis/sega2f.htm

//The CRAM contains 128 bytes, addresses 0 to 7FH.  For word wide writes to the CRAM, use:
//D15 ~ D0 are valid when we use word for data set.  If the writes are byte wide, write the high byte to $C00000 and the low byte to $C00001.  A long word wide access is equivalent to two sequential word wide accesses.  Place the first data in D31 - D16 and the second data in D15 - D0.  The date may be written sequentially;  the address is incremented by the value of REGISTER #15 after every write, independent of whether the width is byte of word.
//Note that A0 is used in the increment but not in address decoding, resulting in some interesting side-effects if writes are attempted at odd addresses.
	private void cramWrite(int data) {
		if (!cramWrite2) {
			cramWriteData = data;
			cramWrite2 = true;
		} else {
			cramWrite2 = false;
			int word = (cramWriteData << 16) | data;
			
			int index = nextFIFOReadEntry;
			int address = addressPort;
			
			long first =  all >> 16;
			long second = all & 0xFFFF;
			
			int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
			int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 13));
			
			int offset = address + autoIncrementTotal;
			
			int data1 = (word >> 24) & 0xFF;
			int data2 = (word >> 16) & 0xFF;
			
			cram[offset] 	 = data1;
			cram[offset + 1] = data2;
			fifoAddress[index] = offset;
			fifoCode[index] = code;
			fifoData[index] = (data1 << 8) | data2;
			
			int data3 = (word >> 8) & 0xFF;
			int data4 = (word >> 0) & 0xFF;
			
			cram[offset + 2] = data3;
			cram[offset + 3] = data4;
			
			int incrementOffset = autoIncrementTotal + autoIncrementData;
			
			address = address + incrementOffset;	// FIXME wrap
			index = (index + 1) % 4;
			fifoAddress[index] = address;
			fifoCode[index] = code;
			fifoData[index] = (data3 << 8) | data4;
			
			nextFIFOReadEntry = (index + 1) % 4;
			nextFIFOWriteEntry = (index + 1) % 4;
			autoIncrementTotal = incrementOffset + 2;
		}
	}
	
	private void vsramWrite(int data) {
//			int word = (vsramWriteData << 16) | data;
		int word = data;
			
		int index = nextFIFOReadEntry;
		int address = addressPort;
		
		long first =  all >> 16;
		long second = all & 0xFFFF;
		
		int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 13));
		
		int offset = address + autoIncrementTotal;
		
		int data1 = (word >> 8) & 0xFF;
		int data2 = word & 0xFF;
		
		//	si la direccion a escribir se pasa del limite, se pone en el fifo buffer, pero no escribe nada
		if (offset < 0x50) {
			vsram[offset] = data1;
		}
		if (offset < 0x50) {
			vsram[offset + 1] = data2;
		}
		
		fifoAddress[index] = offset;
		fifoCode[index] = code;
		fifoData[index] = (data1 << 8) | data2;
		
//		int data3 = (word >> 8) & 0xFF;
//		int data4 = (word >> 0) & 0xFF;
//		
//		vsram[offset + 2] = data3;
//		vsram[offset + 3] = data4;
		
		int incrementOffset = autoIncrementTotal + autoIncrementData;
		
//		address = address + incrementOffset;	// FIXME wrap
//		index = (index + 1) % 4;
//		fifoAddress[index] = address;
//		fifoCode[index] = code;
//		fifoData[index] = (data3 << 8) | data4;
		
		nextFIFOReadEntry = (index + 1) % 4;
		nextFIFOWriteEntry = (index + 1) % 4;
		autoIncrementTotal = incrementOffset;
	}
	
	public final String pad(int reg) {
        String s = Integer.toHexString(reg).toUpperCase();
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }

    public final String pad4(int reg) {
        String s = Integer.toHexString(reg).toUpperCase();
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }
	
}
