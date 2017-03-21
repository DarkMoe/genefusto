package gen;

import gen.addressing.AddressingMode;
import gen.instruction.Operation;
import m68k.cpu.Size;

public class Gen68 {
	
	//	D0-D7
	private long[] D = new long[8];
	//	A0-A7	(A7 = USP = User Stack Pointer o SSP)
	private long[] A = new long[8];
	
	public long PC;

	//	Supervisor SP
	public int SSP;
	//	User SP
	public long USP;
	
	//	http://tict.ticalc.org/docs/68kguide.txt
	//	Status Register o condition code register
	//	X—Extend: Set to the value of the C-bit for arithmetic operations; otherwise not affected or set to a specified result.
	//	N—Negative: Set if the most significant bit of the result is set; otherwise clear.
	//	Z—Zero: Set if the result equals zero; otherwise clear.
	//	V—Overflow: Set if an arithmetic overflow occurs implying that the result cannot be represented in the operand size; otherwise clear. 
	//	C—Carry: Set if a carry out of the most significant bit of the operand occurs for an addition, or if a borrow occurs in a subtraction; otherwise clear. 
	
//	 Bit 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
//	    -------------------------------------------------
//	    | T| -| S| -| -| I2,1,0 | -| -| -| X| N| Z| V| C|   - the status register
//	    -------------------------------------------------

//Bit 15: T - The trace bit. If it's set an interrupts will be called after
//        each instruction. Often used in debuggers.
//
//Bit 13: S - Supervisor bit. When this bit is set, you have more "access" to
//        some instructions and also to the systembyte. The reason for this is
//        that it prevents programs to disturb the OS with some instructions
//        that you shouldn't use if you're not writing an OS. Is enabled
//        when interruptions are generated.
//
//Bit 8-10: The interrupt mask
//
// The I0, I1 and I2 bits of the system register are used to set the interrupt
//mask: in fact, it means that they are set to an interupt level: if the trap 
//generated has a level higher than the interrupt mask, then the trap is
//executed. Otherwise it is ignored. ( ignoring a trap generally means that
//another interrupt, with a higher priority is beeing treated )
//
//Here is how these bits are set:
//
//        | I2 | I1 | I0 |
//
//level 0 |  0    0    0 |  ---------> lowest priority
//level 1 |  0    0    1 |
//level 2 |  0    1    0 |
//level 3 |  0    1    1 |
//level 4 |  1    0    0 |
//level 5 |  1    0    1 |
//level 6 |  1    1    0 |
//level 7 |  1    1    1 |  ---------> highest priority
	public int SR;
	
	//	8 Floating-Point Data Registers (FP7 – FP0) 
	float[] FP = new float[8];
	//	16-Bit Floating-Point Control Register (FPCR) 
	float FPCR;
	//	32-Bit Floating-Point Status Register (FPSR) 
	float FPSR;
	//	32-Bit Floating-Point Instruction Address Register (FPIAR) 
	float FPIAR;
	
	public GenBus bus;
	
	Gen68(GenBus bus) {
		this.bus = bus;
	}
	
	int cycles = 0;
	
	GenInstruction[] instructions = new GenInstruction[0x10000];
	AddressingMode addressingModes[];
	
	public int runInstruction() {
		long opcode = (bus.read(PC) << 8);
		opcode |= bus.read(PC + 1);
		
		System.out.println(pad4((int) PC) + " - Opcode: " + pad4((int) opcode) + " - SR: " + pad4(SR));
		for (int j = 0; j < 8; j++) {
			System.out.print(" D" + j + ":" + Integer.toHexString((int) D[j]));
		}
		System.out.println();
		for (int j = 0; j < 8; j++) {
			System.out.print(" A" + j + ":" + Integer.toHexString((int) A[j]));
		}
		System.out.println();
		
		if (PC == 0x3c2) {
			bus.vdp.vip = 1;	// hack horrible vblank
			System.out.println();
		}
		
		cycles = 0;
		
		if (PC == 0x3c2) {
			System.out.println();
		}
		
		if (D[7] == 0) {
			System.out.println();
		}
		
 		GenInstruction instruction = instructions[(int) opcode];
		instruction.run((int) opcode);
		
		PC += 2;
		
		return 0;
	}
	
	public void setAByte(int register, long data) {
		long reg = A[register];
		A[register] = ((reg & 0xFFFF_FF00) | (data & 0xFF));
		
		if (register == 7) {
			SSP = (int) A[register];
		}
	}
	
	public void setAWord(int register, long data) {
		long reg = A[register];
		A[register] = ((reg & 0xFFFF_0000) | (data & 0xFFFF));
		
		if (register == 7) {
			SSP = (int) A[register];
		}
	}
	
	public void setALong(int register, long data) {
		A[register] = data & 0xFFFF_FFFFL;
		
		if (register == 7) {
			SSP = (int) A[register];
		}
	}
	
	public void setDByte(int register, long data) {
		long reg = D[register];
		D[register] = ((reg & 0xFFFF_FF00) | (data & 0xFF));
	}
	
	public void setDWord(int register, long data) {
		long reg = D[register];
		D[register] = ((reg & 0xFFFF_0000) | (data & 0xFFFF));
	}
	
	public void setDLong(int register, long data) {
		D[register] = data & 0xFFFF_FFFFL;
	}
	
	public long getA(int register) {
		return A[register] & 0xFFFF_FFFFL;
	}
	
	public long getD(int register) {
		return D[register] & 0xFFFF_FFFFL;
	}

	public void setX() {
		SR = bitSet(SR, 4);
	}
	
	public void clearX() {
		SR = bitReset(SR, 4);
	}
	
	public void setN() {
		SR = bitSet(SR, 3);
	}
	
	public void clearN() {
		SR = bitReset(SR, 3);
	}
	
	public void setZ() {
		SR = bitSet(SR, 2);
	}
	
	public void clearZ() {
		SR = bitReset(SR, 2);
	}
	
	public boolean isZ() {
		return bitTest(SR, 2);
	}
	
	public void setV() {
		SR = bitSet(SR, 1);
	}
	
	public void clearV() {
		SR = bitReset(SR, 1);
	}
	
	public void setC() {
		SR = bitSet(SR, 0);
	}
	
	public void clearC() {
		SR = bitReset(SR, 0);
	}
	
	public boolean isC() {
		return bitTest(SR, 0);
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

    public void reset() {
    	SSP = 0;
    	PC = 0;
    }
    
	public void initialize() {
		//	the processor fetches an initial stack pointer from locations $000000-$000003
		SSP |= (bus.read(0) << 24);
		SSP |= (bus.read(1) << 16);
		SSP |= (bus.read(2) << 8);
		SSP |= (bus.read(3) << 0);
		
		USP = 0xFFFFFFFF;

		//	initial PC specified by locations $000004-$000007
		PC |= (bus.read(4) << 24);
		PC |= (bus.read(5) << 16);
		PC |= (bus.read(6) << 8);
		PC |= (bus.read(7) << 0);
		
		for (int i = 0; i < A.length; i++) {
			A[i] = 0xFFFFFFFF;
			D[i] = 0xFFFFFFFF;
		}
		A[7] = SSP;
		SR = 0x7FFF;
	}
	
	public Operation resolveAddressingMode(Size size, int mode, int register) {
		return resolveAddressingMode(PC + 2, size, mode, register);
	}
	
	public Operation resolveAddressingMode(long offset, Size size, int mode, int register) {
		long addr = 0;
		long data = 0;
		
		AddressingMode addressing = getAddressingMode(mode, register);
		Operation oper = new Operation();
		oper.setRegister(register);
		oper.setAddressingMode(addressing);
		
		if (mode == 0b000) {	//	Dn
			if (size == Size.byt) {		//	byte
				data = D[register] & 0xFF;
			} else if (size == Size.word) {		//	word
				data = D[register] & 0xFFFF;
			} else if (size == Size.longW) {		//	long
				data = D[register];
			}
			
		} else if (mode == 0b001) {		//	An		Address Register Direct Mode 
			if (size == Size.byt) {		//	byte
				data = A[register] & 0xFF;
			} else if (size == Size.word) {		//	word
				data = A[register] & 0xFFFF;
			} else if (size == Size.longW) {		//	long
				data = A[register];
			}
			
		} else if (mode == 0b010) {				//	(An)	Address Register Indirect Mode
			addr = A[register];
			
			oper.setAddress(addr);
			
//			if (size == Size.byt) {				// size = Byte TODO checkear esto
//				data = (bus.read(addr));
//			
//			} else if (size == Size.word) {		// size = word
//				data  = (bus.read(addr) << 8);
//				data |= (bus.read(addr + 1));
//			
//			} else if (size == Size.longW) {	//	size = Long
//				data  = (bus.read(addr)     << 24);
//				data |= (bus.read(addr + 1) << 16);
//				data |= (bus.read(addr + 2) << 8);
//				data |= (bus.read(addr + 3) << 0);
//				
//			}
		} else if (mode == 0b011) {		//	(An)+	 Address Register Indirect with Postincrement Mode 
			addr = A[register];
			oper.setAddress(addr);
			
			if (size == Size.byt) {	//	byte
//				data = bus.read(addr);
				A[register]++;
				
			} else if (size == Size.word) {	//	word
//				data  = (bus.read(addr)     << 8);
//				data |= (bus.read(addr + 1) << 0);
				A[register] += 2;
				
			} else if (size == Size.longW) {	//	long
//				data  = (bus.read(addr)     << 24);
//				data |= (bus.read(addr + 1) << 16);
//				data |= (bus.read(addr + 2) << 8);
//				data |= (bus.read(addr + 3) << 0);
				A[register] += 4;
				
			}
				
		} else if (mode == 0b100) {		//	-(An)	 Address Register Indirect with Predecrement Mode 
			addr = A[register];
			oper.setAddress(addr);
			
			if (size == Size.byt) {	//	byte
				addr--;
				A[register] = addr;
//				data = bus.read(addr);
				
			} else if (size == Size.word) {	//	word
				addr -= 2;
				A[register] = addr;
//				data  = (bus.read(addr)     << 8);
//				data |= (bus.read(addr + 1) << 0);
				
			} else if (size == Size.longW) {	//	long
				addr -= 4;
				A[register] = addr;
//				data  = (bus.read(addr)     << 24);
//				data |= (bus.read(addr + 1) << 16);
//				data |= (bus.read(addr + 2) << 8);
//				data |= (bus.read(addr + 3) << 0);
				
			}
			
		} else if (mode == 0b101) {	//	(d16,An)	Address with Displacement
			long base = A[register];
			long displac = bus.read(PC + 2) << 8;
			displac |= bus.read(PC + 3);
			
			long displacement = (long) displac;
			if ((displacement & 0x8000) > 0) {
				displacement |= 0xFFFF_0000L;	// sign extend 32 bits
			}
			addr = (base + displacement);	// TODO wrap ?
//			data = bus.read(addr);
			
			oper.setAddress(addr);
			
			PC += 2;
			
		} else if (mode == 0b111) {
			if (register == 0b000) {		//	Abs.W
				addr  = (bus.read(offset) << 8);
				addr |= (bus.read(offset + 1) << 0);
			
				if ((addr & 0x8000) > 0) {
					addr |= 0xFFFF_0000;
				}
				
				if (size == Size.byt) {
//					data = bus.read(addr);
				} else if (size == Size.word) {
//					data = (bus.read(addr) << 8);
//					data |= bus.read(addr + 1);
				} else {
					throw new RuntimeException("AA");
				}
				
				oper.setAddress(addr);
				
				PC += 2;
				
			} else if (register == 0b001) {		//	Abs.L
				addr  = (bus.read(offset) << 24);
				addr |= (bus.read(offset + 1) << 16);
				addr |= (bus.read(offset + 2) << 8);		
				addr |= (bus.read(offset + 3));
//				data = (bus.read(addr));
				
				oper.setAddress(addr);
				
				PC += 4;
				
				cycles = 10;
				
			} else if (register == 0b010) {		//	 (d16,PC)	Program Counter Indirect with Displacement Mode
				long displacement = (bus.read(PC + 2) << 8);
				displacement 	 |= (bus.read(PC + 3));
				
				addr = PC + 2 + displacement;
				oper.setAddress(addr);
				
				PC += 2;
				// TODO check otros sizes

			} else if (register == 0b011) {		 //	(d8,PC,Xi)		PC With index operan
				long exten  = (bus.read(PC + 2) << 8);
				     exten |= (bus.read(PC + 3));
				int displacement = (int) (exten & 0xFF);		// es 8 bits, siempre el ultimo byte ?
				
				if ((displacement & 0x80) > 0) { 	// sign extend
					displacement = 0xFFFF_FF00 | displacement;
				}
				int idxRegNumber = (int) ((exten >> 12) & 0x07);
				Size idxSize = ((exten & 0x0800) == 0x0800 ? Size.longW : Size.word);
				boolean idxIsAddressReg = ((exten & 0x8000) == 0x8000);
				
				if (idxIsAddressReg) {
					if (idxSize == Size.word) {
						data = getA(idxRegNumber);
						if ((data & 0x8000) > 0) {
							data = 0xFFFF_0000 | data;
						}
					} else {
						data = getA(idxRegNumber);
					}
				} else {
					if (idxSize == Size.word) {
						data = getD(idxRegNumber);
						if ((data & 0x8000) > 0) {
							data = 0xFFFF_0000 | data;
						}
					} else {
						data = getD(idxRegNumber);
					}
				}
				
				long result = data = PC + 2 + displacement + data;
				oper.setAddress(result);
				
				PC += 2;
				
			} else if (register == 0b100) {		//	#data
				
				oper.setAddress(PC + 2);
				
				if (size == Size.byt) {		//	aunque sea byte, siempre ocupa 2 bytes y cuenta el de la derecha
//					data  = (bus.read(PC + 2) << 8);
//					data |= (bus.read(PC + 3) << 0);
					
//					data = data & 0xFF;
					
					PC += 2;
				} else if (size == Size.word) {
//					data  = (bus.read(PC + 2) << 8);
//					data |= (bus.read(PC + 3) << 0);
					
					PC += 2;
					
				} else if (size == Size.longW) {	// long
//					data  = (bus.read(PC + 2) << 24);
//					data |= (bus.read(PC + 3) << 16);
//					data |= (bus.read(PC + 4) << 8);
//					data |= (bus.read(PC + 5));
					
					PC += 4;
					
				}
				
			} else {
				throw new RuntimeException("Addressing no soportado: " + mode + " REG: " + register);
			}
		} else {
			throw new RuntimeException("Addressing no soportado: " + mode);
		}
		
//		oper.setData(data);
		
		return oper;
	}
	
	private AddressingMode getAddressingMode(int mode, int register) {
		AddressingMode addr;
		if (mode < 7) {
			addr = addressingModes[mode];
		} else {
			addr = addressingModes[mode + register];
		}
		if (addr == null) {
			throw new RuntimeException("ADDR MODE NOT ! " + mode + " " + register);
		}
		return addr;
	}

	public void writeKnownAddressingMode(Operation o, long data, Size size) {
		AddressingMode addressing = o.getAddressingMode();
		
		o.setData(data);
		
		if (Size.byt == size) {
			addressing.setByte(o);
		} else if (Size.word == size) {
			addressing.setWord(o);
		} else if (Size.longW == size) {
			addressing.setLong(o);
		}
		
//		case dataRegisterDirect:
//			int dataReg = o.getRegister();
//			if (size == Size.byt) {
//				long old = D[dataReg];
//				long v = (old & (0xFFFF_FF00)) | data;
//				D[dataReg] = v;
//				
//			} else if (size == Size.word) {
//				long old = D[dataReg];
//				long v = (old & (0xFFFF_0000)) | data;
//				D[dataReg] = v;
//				
//			} else if (size == Size.longW) {
//				D[dataReg] = data;
//			}
//			break;
//			
//		case addressRegisterInderect:
//			if (size == Size.longW) {
//				bus.write(o.getAddress(), (data >> 16) & 0xFFFF, size);
//				bus.write(o.getAddress() + 2, data & 0xFFFF, size);
//			} else {
//				bus.write(o.getAddress(), data, size);
//			}
//			break;
//			
//		case addressRegisterIndirectPostIncrement:
//			if (size == Size.longW) {
//				bus.write(o.getAddress(), (data >> 16) & 0xFFFF, size);
//				bus.write(o.getAddress() + 2, data & 0xFFFF, size);
//			} else {
//				bus.write(o.getAddress(), data, size);
//			}
//			break;
//			
//		case addressRegisterIndirectPreIncrement:
//			if (size == Size.longW) {
//				bus.write(o.getAddress(), (data >> 16) & 0xFFFF, size);
//				bus.write(o.getAddress() + 2, data & 0xFFFF, size);
//			} else {
//				bus.write(o.getAddress(), data, size);
//			}
//			break;
//			
//		case absoluteWord:
//			if (size == Size.longW) {
//				bus.write(o.getAddress(), (data >> 16) & 0xFFFF, size);
//				bus.write(o.getAddress() + 2, data & 0xFFFF, size);
//			} else {
//				bus.write(o.getAddress(), data, size);
//			}
//			break;
//			
//		default:
//			throw new RuntimeException("NO ! " + o.getAddressingMode().toString());
//		}
	}
	
	public void writeAddressingMode(Size size, long offset, long data, int mode, int register) {
		long addr;
		
		if (mode == 0b000) {	//	Dn
			if (size == Size.byt) {
				long old = D[register];
				long v = (old & (0xFFFF_FF00)) | data;
				D[register] = v;	// size = byte, se escribe el ultimo byte
				
			} else if (size == Size.word) {
				long old = D[register];
				long v = (old & (0xFFFF_0000)) | data;
				D[register] = v;	// size = word, se escribe los 2 ultimos bytes
				
			} else if (size == Size.longW) {
				D[register] = data;
			}
			
		} else if (mode == 0b001) {		//	An		Address Register Direct Mode 
			
			throw new RuntimeException("IMPL");
			
		} else if (mode == 0b010) {		//	(An)	Address Register Indirect Mode
			addr = A[register];
			
			if (size == Size.longW) {
				bus.write(addr, (data >> 16), size);
				bus.write(addr + 2, (data & 0xFFFF), size);
				
			} else if (size == Size.word) {
				bus.write(addr, (data & 0xFFFF), size);
				
			} else if (size == Size.byt) {
				bus.write(addr, (data & 0xFF), size);
			}
			
		} else if (mode == 0b011) {		//	(An)+	 Address Register Indirect with Postincrement Mode 
			addr = A[register];
			
			if (size == Size.byt) {
				bus.write(addr, data & 0xFF, size);
				A[register]++;
			} else if (size == Size.word) {
				bus.write(addr, data & 0xFFFF, size);
				A[register] += 2;
			} else if (size == Size.longW) {
				bus.write(addr, data >> 16, size);
				bus.write(addr + 2, data & 0xFFFF, size);
				A[register] += 4;
			}
		} else if (mode == 0b100) {		//	-(An)
			long address = A[register];
			
			if (size == Size.longW) {	//	long word
				address = (address - 4) & 0xFFFF_FFFF;
				A[register] = address;	// predecrement
				
				bus.write(address, (data >> 16) & 0xFFFF, size);
				bus.write(address + 2, (data & 0xFFFF), size);
				
			} else {
				throw new RuntimeException("NOT " + size);
			}
			
		} else if (mode == 0b101) {		//	(d16,An)	Address with Displacement
			if (size == Size.byt) {	//	byte
				long base = A[register];
				long displac = (bus.read(offset) << 8);
				displac 	|= (bus.read(offset + 1));
				
				if ((displac & 0x80) > 0) {
					displac |= 0xFFFF_FF00L;	// sign extend 32 bits
				}
				addr = (base + displac);
				bus.write(addr, data & 0xFF, size);
				
				PC += 2;
				
			} else if (size == Size.word) {
				throw new RuntimeException("NOO");
			} else if (size == Size.longW) {
				throw new RuntimeException("NOO");
			}
		
		} else if (mode == 0b110) {		//	 Address Register Indirect with Index (Base Displacement) Mode
//			int ext = fetchPCWordSigned();
//			displacement = signExtendByte(ext);
//			idxRegNumber = (ext >> 12) & 0x07;
//			idxSize = ((ext & 0x0800) == 0x0800 ? Size.Long : Size.Word);
//			idxIsAddressReg = ((ext & 0x8000) == 0x8000);
//			int idxVal;
//			if(idxIsAddressReg)
//			{
//				if(idxSize == Size.Word)
//				{
//					idxVal = getAddrRegisterWordSigned(idxRegNumber);
//				}
//				else
//				{
//					idxVal = getAddrRegisterLong(idxRegNumber);
//				}
//			}
//			else
//			{
//				if(idxSize == Size.Word)
//				{
//					idxVal = getDataRegisterWordSigned(idxRegNumber);
//				}
//				else
//				{
//					idxVal = getDataRegisterLong(idxRegNumber);
//				}
//			}
//			address = getAddrRegisterLong(regNumber) + displacement + idxVal;
			
		} else if (mode == 0b111) {
			if (register == 0b000) {			//	Abs.W
				addr  = (bus.read(offset) << 8);
				addr |= (bus.read(offset + 1) << 0);
			
				if ((addr & 0x8000) > 0) {
					addr |= 0xFFFF_0000;
				}
				
				if (size == Size.byt) {
					bus.write(addr, data, size);
				} else if (size == Size.word) {
					bus.write(addr, data, size);
				} else if (size == Size.longW) {
					bus.write(addr, (data >> 16), size);
					bus.write(addr + 2, data & 0xFFFF, size);
				}
				
				PC += 2;
				
			} else if (register == 0b001) {		//	Abs.L
				addr  = (bus.read(offset) << 24);
				addr |= (bus.read(offset + 1) << 16);
				addr |= (bus.read(offset + 2) << 8);
				addr |= (bus.read(offset + 3) << 0);
			
				if (size == Size.byt) {
					bus.write(addr, data & 0xFF, size);
					
				} else if (size == Size.word) {
					bus.write(addr, data & 0xFFFF, size);
					
				} else if (size == Size.longW) {
					bus.write(addr, data >> 16, size);
					bus.write(addr + 2, data & 0xFFFF, size);
					
				}
				
				PC += 4;
				
			} else if (register == 0b010) {		//	(d16,PC)	Program Counter Indirect with Displacement Mode
				throw new RuntimeException("IMPL");
			} else if (register == 0b100) {		//	#data
				throw new RuntimeException("IMPL");
			} else {
				throw new RuntimeException("Addressing no soportado: " + mode + " REG: " + register);
			}
		} else {
			throw new RuntimeException("Addressing no soportado: " + mode);
		}
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
    
    int getInterruptMask() {
    	return (SR >> 8) & 0x7;
    }

    int totalInstructions = 0;
    
	public void addInstruction(int opcode, GenInstruction ins) {
		GenInstruction instr = instructions[opcode];
		if (instr != null) {
			throw new RuntimeException(pad4(opcode));
		}
		totalInstructions++;
		instructions[opcode] = ins;
	}
	
}
