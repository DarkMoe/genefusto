package gen;

public class GenVdp {

	int[] vram  = new int[0x10000];
	int[] cram  = new int[0x80];		//	The CRAM contains 128 bytes, addresses 0 to 7F
	int[] vsram = new int[0x50];		//	The VSRAM contains 80 bytes, addresses 0 to 4F
	

//	VSRAM
//	 The VDP has 40x10 bits of on-chip vertical scroll RAM. It is accessed as
//	 40 16-bit words through the data port. Each word has the following format:
//
//	 ------yyyyyyyyyy
//
//	 y = Vertical scroll factor (0-3FFh)
//
//	 When accessing VSRAM, only address bits 6 through 1 are valid.
//	 The high-order address bits are ignored. Since VSRAM is word-wide, address
//	 bit zero has no effect.
//
//	 Even though there are 40 words of VSRAM, the address register will wrap
//	 when it passes 7Fh. Writes to the addresses beyond 50h are ignored.
	
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
	boolean memToVram = false;
	
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
	
	GenBus bus;
	
	public GenVdp(GenBus bus) {
		this.bus = bus;
	}
	
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
				if ((disp) && ((data & 0x40) == 0x40)) {	// el display estaba prendido pero se apago
					for (int i = 0; i < 320; i++) {
						for (int j = 0; j < 256; j++) {
							screenData[i][j] = 0;
							bus.emu.renderScreen();
						}
					}
				}
				
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
				int addr = (int) ((first & 0x3FFF) | ((second & 0x3) << 14));
				
				addressPort = addr;
				autoIncrementTotal = 0;	// reset este acumulador
				
				int addressMode = code & 0xF;	// solo el primer byte, el bit 4 y 5 son para DMA
												// que ya fue contemplado arriba
				if (addressMode == 0b0000) { // VRAM Read
					throw new RuntimeException("ADDR WRITE !");

				} else if (addressMode == 0b0001) { // VRAM Write
					vramWrite = true;
					cramWrite = false;
					vsramWrite = false;

				} else if (addressMode == 0b1000) { // CRAM Read
					throw new RuntimeException("ADDR WRITE !");

				} else if (addressMode == 0b0011) { // CRAM Write
					cramWrite = true;
					vramWrite = false;
					vsramWrite = false;

				} else if (addressMode == 0b0100) { // VSRAM Read
					throw new RuntimeException("ADDR WRITE !");

				} else if (addressMode == 0b0101) { // VSRAM Write
					vsramWrite = true;
					vramWrite = false;
					cramWrite = false;

				}
				
				//	https://wiki.megadrive.org/index.php?title=VDP_DMA
				if ((code & 0b100000) > 0) { // DMA
					int dmaBits = code >> 4;
					dmaRecien = true;
						
					if ((dmaBits & 0b10) > 0) {		//	VRAM Fill
						if ((registers[0x17] & 0x80) == 0x80) {
//						FILL mode fills with same data from free even VRAM address.
//						FILL for only VRAM.
							dmaModo = DmaMode.VRAM_FILL;
							vramFill = true;
							
						} else {
							dmaModo = DmaMode.MEM_TO_VRAM;
							memToVram = true;
							
							if (m1) {
								dmaMem2Vram(all);
							}
						}
						
					} else if ((dmaBits & 0b11) > 0) {		//	VRAM Copy
						dmaModo = DmaMode.VRAM_COPY;
						
					}
				}
			}
		}
	}
	
	boolean dmaRecien = false;
	
	public void dmaOperation() {
		if (dma == 1) {
			int dmaLength = (dmaLengthCounterHi << 8) | dmaLengthCounterLo;
			
			int index, data, destAddr;
			if (dmaRecien) {
				System.out.println("DMA MODE: " + dmaModo);
				
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
			
//			System.out.println(pad4(dmaLength));
			
			if (vramWrite) {
				if (destAddr == 0x26) {
					System.out.println();
				}
				
				vram[destAddr] = data >> 8;
				vram[destAddr + 1] = data & 0xFF;
			} else if (cramWrite) {
				if (data == 0xEEE) {
					System.out.println();
				}
				cram[destAddr] = data >> 8;
				cram[destAddr + 1] = data & 0xFF;
			} else if (vsramWrite) {
				vsram[destAddr] = data >> 8;
				vsram[destAddr + 1] = data & 0xFF;
			} else {
				throw new RuntimeException("not");
			}
			
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
	
	DmaMode dmaModo;
	
	enum DmaMode {
		MEM_TO_VRAM, VRAM_FILL, VRAM_COPY; 
	}
	
	boolean dmaRequested;
	
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
			
//		} else if (memToVram) {
//			if (m1) {
//				memToVram = false;
//				
//				dmaMem2Vram();
//				
//				return;
//			} else {
//				System.out.println("M1 should be 1 in the DMA transfer. otherwise we can't guarantee the operation.");
//			}
			
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

//	 Registers 19, 20, specify how many 16-bit words to transfer:
//
//	 #19: L07 L06 L05 L04 L03 L02 L01 L00
//	 #20: L15 L14 L13 L12 L11 L10 L08 L08
//
//	 Note that a length of 7FFFh equals FFFFh bytes transferred, and a length
//	 of FFFFh = 1FFFF bytes transferred.
//
//	 Registers 21, 22, 23 specify the source address on the 68000 side:
//
//	 #21: S08 S07 S06 S05 S04 S03 S02 S01
//	 #22: S16 S15 S14 S13 S12 S11 S10 S09
//	 #23:  0  S23 S22 S21 S20 S19 S18 S17
//
//	 If the source address goes past FFFFFFh, it wraps to FF0000h.
//	 (Actually, it probably wraps at E00000h, but there's no way to tell as
//	  the two addresses are functionally equivelant)
//
//	 When doing a transfer to CRAM, the operation is aborted once the address
//	 register is larger than 7Fh. The only known game that requires this is
//	 Batman & Robin, which will have palette corruption in levels 1 and 3
//	 otherwise. This rule may possibly apply to VSRAM transfers as well.
	
//	 The following events occur after the command word is written:
//
//		 - 68000 is frozen.
//		 - VDP reads a word from source address.
//		 - Source address is incremented by 2.
//		 - VDP writes word to VRAM, CRAM, or VSRAM.
//		   (For VRAM, the data is byteswapped if the address register has bit 0 set)
//		 - Address register is incremented by the value in register #15.
//		 - Repeat until length counter has expired.
//		 - 68000 resumes operation.
	private void dmaMem2Vram(long commandWord) {
		int dmaLength = (dmaLengthCounterHi << 8) | dmaLengthCounterLo;
		
		long sourceAddr = ((registers[0x17] & 0x7F) << 16) | (registers[0x16] << 8) | (registers[0x15]);
		long sourceTrue = sourceAddr << 1;	// duplica, trabaja asi
		int destAddr = (int) (((commandWord & 0x3) << 14) | ((commandWord & 0x3F00_0000L) >> 16));
		
		int index, data;
		while (dmaLength > 0) {
//			currentFIFOReadEntry = nextFIFOReadEntry;
//			currentFIFOWriteEntry = nextFIFOWriteEntry;
//			index = currentFIFOReadEntry;
//			data = dataPort;
//			destAddr = addressPort;
//			
//			fifoData[index] = data;

			//TODO el pipe
			
			int data1 = (int) bus.read(sourceTrue);
			int data2 = (int) bus.read(sourceTrue + 1);
			
			if (vramWrite) {
				if (destAddr == 0x26) {
					System.out.println();
				}
				
				vram[destAddr] = data1;
				vram[destAddr + 1] = data2;
			} else if (cramWrite) {
				if (data2 == 0xEE){
					System.out.println();
				}
				
				cram[destAddr] = data1;
				cram[destAddr + 1] = data2;
			} else if (vsramWrite) {
				vsram[destAddr] = data1;
				vsram[destAddr + 1] = data2;
			} else {
				throw new RuntimeException("not");
			}
			
			sourceTrue += 2;
			destAddr += registers[15];
			
			dmaLength--;
		}
		
		int newSource = (int) (sourceTrue >> 1);
		registers[0x17] = ((registers[0x17] & 0x80) | ((newSource >> 16) & 0x7F));
		registers[0x16] = (newSource >> 8) & 0xFF;
		registers[0x15] = newSource & 0xFF;
				
//				int code = (int) ((first >> 14) | (((second >> 4) & 0xF) << 2));
		
//				fifoCode[index] = code;		// TODO implementar el code como esta en los otros writes de memory
			
		
			
//		dmaLength = dmaLength & 0xFFFF;
//		dmaLengthCounterHi = dmaLength >> 8;
//		dmaLengthCounterLo = dmaLength & 0xFF;
//			
//		addressPort += 2;
//		fifoAddress[index] = destAddr + 2;	//	FIXME, no es fijo, se actualiza en paralelo mientras se siguen ejecutando instrucciones, hay q contar ciclos de cpu
//		
//		if (dmaRecien) {
//			dmaRecien = false;
//			
//			index = (index + 1) % 4;
//			nextFIFOReadEntry = index;
//			nextFIFOWriteEntry = index;
//		}
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
		
		if (offset == 0x26) {
			System.out.println();
		}
		
		vram[offset] 	 = data1;
		vram[offset + 1] = data2;
		
		System.out.println("addr: " + Integer.toHexString(offset) + " - data: " + Integer.toHexString(data1));
		System.out.println("addr: " + Integer.toHexString(offset + 1) + " - data: " + Integer.toHexString(data2));
		
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

    int totalCycles = 0;
    int scanline = 0;
    
	public int[][] screenData = new int[320][256];
    
	public void run(int cycles) {
		totalCycles += cycles;
		if (totalCycles > 251503) {
			totalCycles = 0;
			vip = 1;
			
			if ((registers[1] & 0x40) == 0x40) {
				renderPlaneA();
				renderPlaneB();
				renderWindow();
				
				bus.emu.renderScreen();
			}
		}
		
	}


//Register 02 - Plane A Name Table Location
//7	6		5		4		3		2	1	0
//x	SA16	SA15	SA14	SA13	x	x	x
//	SA15-SA13 defines the upper three bits of the VRAM location of Plane A's nametable. This value is effectively the address divided by $400; however, the low three bits are ignored, so the Plane A nametable has to be located at a VRAM address that's a multiple of $2000. For example, if the Plane A nametable was to be located at $C000 in VRAM, it would be divided by $400, which results in $30, the proper value for this register.
//	SA16 is only valid if 128 KB mode is enabled, and allows for rebasing the Plane A nametable to the second 64 KB of VRAM.
	private void renderPlaneA() {
		int nameTableLocation = registers[2] & 0x38;	// bit 6 para modo extendido de vram, no lo emulo
		nameTableLocation *= 0x400;
		
		int tileLocator = nameTableLocation;
		for (int vertTile = 0; vertTile < 32; vertTile++) {
			for (int horTile = 0; horTile < 40; horTile++) {//	40 words / tiles por scanline
				int loc = tileLocator + (vertTile / (8 * 40));
				
				if (loc == 0xC59C) {
					System.out.println();
				}
				int nameTable = vram[loc] << 8;
				nameTable |= vram[loc + 1];
				
				tileLocator += 2;
			
//				An entry in a name table is 16 bits, and works as follows:
//				15			14 13	12				11		   			10 9 8 7 6 5 4 3 2 1 0
//				Priority	Palette	Vertical Flip	Horizontal Flip		Tile Index
				int tileIndex = (nameTable & 0x07FF);	// cada tile ocupa 32 bytes
				
				boolean horFlip = bitTest(nameTable, 11);
				boolean vertFlip = bitTest(nameTable, 12);
				int paletteLineIndex = (nameTable >> 13) & 0x3;
				boolean priority = bitTest(nameTable, 15);
				
				int paletteLine = paletteLineIndex * 32;	//	16 colores por linea, 2 bytes por color
				
				if (nameTable != 0) {
					System.out.println();
				}
				
				tileIndex *= 0x20;
				
				for (int filas = 0; filas < 8; filas++) {
					for (int k = 0; k < 4; k++) {
						int grab = (tileIndex + k) + (filas * 4);
						int data = vram[grab];
						
						if (data != 0) {
							System.out.println();
						}
						
						int pixel1 = (data & 0xF0) >> 4;
						int pixel2 = data & 0x0F;
						
						int colorIndex1 = paletteLine + (pixel1 * 2);
						int colorIndex2 = paletteLine + (pixel2 * 2);
						
						int color1 = cram[colorIndex1] << 8 | cram[colorIndex1 + 1];
						int color2 = cram[colorIndex2] << 8 | cram[colorIndex2 + 1];
						
						if (color1 != 0) {
							System.out.println();
						}
						
						int r = (color1 >> 1) & 0x7;
						int g = (color1 >> 5) & 0x7;
						int b = (color1 >> 9) & 0x7;
						
						int r2 = (color2 >> 1) & 0x7;
						int g2 = (color2 >> 5) & 0x7;
						int b2 = (color2 >> 9) & 0x7;
						
						int po = horTile * 8 + (k * 2);
						int pu = vertTile * 8 + (filas);
						
						screenData[po][pu] = getColour(r, g, b);
						screenData[po + 1][pu] = getColour(r2, g2, b2);
					}
				}
			}
			tileLocator += 48;	// fuera del active view, 24 words o 48 bytes
		}
	}
	
	private int getColour(int red, int green, int blue) {
        int extrapoRed = ((red + 1) * 32) - 1;						//	el maximo (1F) es 31 en decimal, al multiplicarlo por 8 da 248 (el blanco puro siendo 255)
        int extrapoGreen = ((green + 1) * 32) - 1;					//	asi esta extrapolado casi perfecto y linealmente. Solo queda el caso del negro, que en lugar
        int extrapoBlue = ((blue + 1) * 32) - 1;						// de ser 0, queda en 7 (se puede poner un if y ya)
        
        int elco = extrapoRed << 16 | extrapoGreen << 8 | extrapoBlue;
        
        return elco;
	}
	
	private void renderPlaneB() {
		
	}

	private void renderWindow() {
		
	}

	public boolean bitTest(long address, int position) {
        return ((address & (1 << position)) != 0);
    }

    public int bitSet(int address, int position) {
        return address | (1 << position);
    }

    public int bitReset(int address, int position) {
        return address & ~(1 << position);
    }
	
}
