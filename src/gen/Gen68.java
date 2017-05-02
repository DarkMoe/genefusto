package gen;

import gen.addressing.AddressingMode;
import gen.instruction.Operation;

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
	
	StringBuilder sb = new StringBuilder();
	public boolean print;
	
	public int runInstruction() {
		long opcode = (bus.read(PC) << 8);
		opcode |= bus.read(PC + 1);
		
		sb.append(pad4((int) PC) + " - Opcode: " + pad4((int) opcode) + " - SR: " + pad4(SR) + "\r\n");
		for (int j = 0; j < 8; j++) {
			sb.append(" D" + j + ":" + Integer.toHexString((int) D[j]));
		}
		sb.append("\r\n");
		for (int j = 0; j < 8; j++) {
			sb.append(" A" + j + ":" + Integer.toHexString((int) A[j]));
		}
		sb.append("\r\n");
		if (print) {
			System.out.println(sb.toString());
		}
		
		sb.setLength(0);
		
		cycles = 0;
		
//		print = true;
		
		if (PC == 0x3f44) {
			System.out.println();
//			print = false;
		}

		if (bus.vdp.vram[0xE000] == 0x80){
			System.out.println();
		}
		
		if (PC == 0x53a) {
			System.out.println();
//			print = true;
		}
		
		if (PC == 0xebd38) {
			System.out.println();
			print = true;
		}
		
 		GenInstruction instruction = getInstruction((int) opcode);
 		instruction.run((int) opcode);
		
		PC += 2;
		
		return 0;
	}
	
	private GenInstruction getInstruction(int opcode) {
		GenInstruction instr = instructions[opcode];
		if (instr == null) {
			System.out.println("PC: " + Integer.toHexString((int) PC) + " - INSTR: " + Integer.toHexString(opcode));
		}
		return instr;
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
	
	public boolean isX() {
		return bitTest(SR, 4);
	}
	
	public void setN() {
		SR = bitSet(SR, 3);
	}
	
	public void clearN() {
		SR = bitReset(SR, 3);
	}
	
	public boolean isN() {
		return bitTest(SR, 3);
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
	
	public boolean isV() {
		return bitTest(SR, 1);
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
		
		USP = 0xFFFF_FFFFL;

		//	initial PC specified by locations $000004-$000007
		PC |= (bus.read(4) << 24);
		PC |= (bus.read(5) << 16);
		PC |= (bus.read(6) << 8);
		PC |= (bus.read(7) << 0);
		
		for (int i = 0; i < A.length; i++) {
			A[i] = 0xFFFF_FFFFL;
			D[i] = 0xFFFF_FFFFL;
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
			if (size == Size.BYTE) {		//	byte
				data = D[register] & 0xFF;
			} else if (size == Size.WORD) {		//	word
				data = D[register] & 0xFFFF;
			} else if (size == Size.LONG) {		//	long
				data = D[register];
			}
			
		} else if (mode == 0b001) {		//	An		Address Register Direct Mode 
			if (size == Size.BYTE) {		//	byte
				data = A[register] & 0xFF;
			} else if (size == Size.WORD) {		//	word
				data = A[register] & 0xFFFF;
			} else if (size == Size.LONG) {		//	long
				data = A[register];
			}
			
		} else if (mode == 0b010) {				//	(An)	Address Register Indirect Mode
			addr = A[register];
			
			oper.setAddress(addr);
			
		} else if (mode == 0b011) {		//	(An)+	 Address Register Indirect with Postincrement Mode 
			addr = getA(register);
			oper.setAddress(addr);
			
			if (size == Size.BYTE) {	//	byte
//				data = bus.read(addr);
				if (register == 7) {	// stack pointer siempre alineado de a 2
					addr += 2;
				} else {
					addr += 1;
				}
				setALong(register, addr);
				
			} else if (size == Size.WORD) {	//	word
//				data  = (bus.read(addr)     << 8);
//				data |= (bus.read(addr + 1) << 0);
				A[register] += 2;
				if (register == 7) {
					SSP = (int) A[register];
				}
				
			} else if (size == Size.LONG) {	//	long
//				data  = (bus.read(addr)     << 24);
//				data |= (bus.read(addr + 1) << 16);
//				data |= (bus.read(addr + 2) << 8);
//				data |= (bus.read(addr + 3) << 0);
				A[register] += 4;
				if (register == 7) {
					SSP = (int) A[register];
				}
				
			}
				
		} else if (mode == 0b100) {		//	-(An)	 Address Register Indirect with Predecrement Mode 
			addr = A[register];
			
			if (size == Size.BYTE) {	//	byte
				if (register == 7) {	// stack pointer siempre alineado de a 2
					addr -= 2;
				} else {
					addr -= 1;
				}
				setALong(register, addr);
				
			} else if (size == Size.WORD) {	//	word
				addr -= 2;
				A[register] = addr;
				if (register == 7) {
					SSP = (int) addr;
				}
//				data  = (bus.read(addr)     << 8);
//				data |= (bus.read(addr + 1) << 0);
				
			} else if (size == Size.LONG) {	//	long
				addr -= 4;
				A[register] = addr;
				if (register == 7) {
					SSP = (int) addr;
				}
//				data  = (bus.read(addr)     << 24);
//				data |= (bus.read(addr + 1) << 16);
//				data |= (bus.read(addr + 2) << 8);
//				data |= (bus.read(addr + 3) << 0);
				
			}
			
			oper.setAddress(addr);
			
		} else if (mode == 0b101) {	//	(d16,An)	Address with Displacement
			long base = A[register];
			long displac = bus.read(PC + 2) << 8;
			displac |= bus.read(PC + 3);
			
			long displacement = (long) displac;
			if ((displacement & 0x8000) > 0) {
				displacement |= 0xFFFF_0000L;	// sign extend 32 bits
			}
			addr = (int) (base + displacement);	// TODO verificar esto, al pasarlo a int hace el wrap bien parece
//			data = bus.read(addr);
			
			oper.setAddress(addr);
			
			PC += 2;
			
		} else if (mode == 0b110) {	//	AddressRegisterWithIndex
			long exten  = (bus.read(PC + 2) << 8);
		     	 exten |= (bus.read(PC + 3));
			int displacement = (int) (exten & 0xFF);		// es 8 bits, siempre el ultimo byte ?
			
			if ((displacement & 0x80) > 0) { 	// sign extend
				displacement = 0xFFFF_FF00 | displacement;
			}
			int idxRegNumber = (int) ((exten >> 12) & 0x07);
			Size idxSize = ((exten & 0x0800) == 0x0800 ? Size.LONG : Size.WORD);
			boolean idxIsAddressReg = ((exten & 0x8000) == 0x8000);
			
			if (idxIsAddressReg) {
				if (idxSize == Size.WORD) {
					data = getA(idxRegNumber) & 0xFFFF;	// confirmar este wrap aca
					if ((data & 0x8000) > 0) {
						data = 0xFFFF_0000 | data;
					}
				} else {
					data = getA(idxRegNumber);
				}
			} else {
				if (idxSize == Size.WORD) {
					data = getD(idxRegNumber) & 0xFFFF;
					if ((data & 0x8000) > 0) {
						data = 0xFFFF_0000 | data;
					}
				} else {
					data = getD(idxRegNumber);
				}
			}
			
			long result = getA(register) + displacement + data;
			oper.setAddress(result);
			
			PC += 2;
			
		} else if (mode == 0b111) {
			if (register == 0b000) {		//	Abs.W
				addr  = (bus.read(offset) << 8);
				addr |= (bus.read(offset + 1) << 0);
			
				if ((addr & 0x8000) > 0) {
					addr |= 0xFFFF_0000L;
				}
				
				if (size == Size.BYTE) {
//					data = bus.read(addr);
				} else if (size == Size.WORD) {
//					data = (bus.read(addr) << 8);
//					data |= bus.read(addr + 1);
				} else {
//					throw new RuntimeException("AA");
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
				
				if ((displacement & 0x8000) > 0) {
					displacement = -displacement;
					displacement &= 0xFFFF;
					addr = PC + 2 - displacement;
				} else {
					addr = PC + 2 + displacement;
				}
				oper.setAddress(addr);
				
				PC += 2;
				// TODO check otros sizes

			} else if (register == 0b011) {		 //	(d8,PC,Xi)		PC With index operand
				long exten  = (bus.read(PC + 2) << 8);
				     exten |= (bus.read(PC + 3));
				int displacement = (int) (exten & 0xFF);		// es 8 bits, siempre el ultimo byte ?
				
				if ((displacement & 0x80) > 0) { 	// sign extend
					displacement = 0xFFFF_FF00 | displacement;
				}
				int idxRegNumber = (int) ((exten >> 12) & 0x07);
				Size idxSize = ((exten & 0x0800) == 0x0800 ? Size.LONG : Size.WORD);
				boolean idxIsAddressReg = ((exten & 0x8000) == 0x8000);
				
				if (idxIsAddressReg) {
					if (idxSize == Size.WORD) {
						data = getA(idxRegNumber) & 0xFFFF;	// confirmar este wrap aca
						if ((data & 0x8000) > 0) {
							data = 0xFFFF_0000 | data;
						}
					} else {
						data = getA(idxRegNumber);
					}
				} else {
					if (idxSize == Size.WORD) {
						data = getD(idxRegNumber) & 0xFFFF;
						if ((data & 0x8000) > 0) {
							data = 0xFFFF_0000 | data;
						}
					} else {
						data = getD(idxRegNumber);
					}
				}
				
				long result = PC + 2 + displacement + data;
				oper.setAddress(result);
				
				PC += 2;
				
			} else if (register == 0b100) {		//	#data
				
				oper.setAddress(PC + 2);
				
				if (size == Size.BYTE) {		//	aunque sea byte, siempre ocupa 2 bytes y cuenta el de la derecha
//					data  = (bus.read(PC + 2) << 8);
//					data |= (bus.read(PC + 3) << 0);
					
//					data = data & 0xFF;
					
					PC += 2;
				} else if (size == Size.WORD) {
//					data  = (bus.read(PC + 2) << 8);
//					data |= (bus.read(PC + 3) << 0);
					
					PC += 2;
					
				} else if (size == Size.LONG) {	// long
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
		
		if (Size.BYTE == size) {
			addressing.setByte(o);
		} else if (Size.WORD == size) {
			addressing.setWord(o);
		} else if (Size.LONG == size) {
			addressing.setLong(o);
		}
	}
		
//	public void writeAddressingMode(Size size, long offset, long data, int mode, int register) {
//		long addr;
//		
//		if (mode == 0b000) {	//	Dn
//			if (size == Size.BYTE) {
//				long old = D[register];
//				long v = (old & (0xFFFF_FF00)) | data;
//				D[register] = v;	// size = byte, se escribe el ultimo byte
//				
//			} else if (size == Size.WORD) {
//				long old = D[register];
//				long v = (old & (0xFFFF_0000)) | data;
//				D[register] = v;	// size = word, se escribe los 2 ultimos bytes
//				
//			} else if (size == Size.LONG) {
//				D[register] = data;
//			}
//			
//		} else if (mode == 0b001) {		//	An		Address Register Direct Mode 
//			
//			throw new RuntimeException("IMPL");
//			
//		} else if (mode == 0b010) {		//	(An)	Address Register Indirect Mode
//			addr = A[register];
//			
//			if (size == Size.LONG) {
//				bus.write(addr, (data >> 16), size);
//				bus.write(addr + 2, (data & 0xFFFF), size);
//				
//			} else if (size == Size.WORD) {
//				bus.write(addr, (data & 0xFFFF), size);
//				
//			} else if (size == Size.BYTE) {
//				bus.write(addr, (data & 0xFF), size);
//			}
//			
//		} else if (mode == 0b011) {		//	(An)+	 Address Register Indirect with Postincrement Mode 
//			addr = A[register];
//			
//			if (size == Size.BYTE) {
//				bus.write(addr, data & 0xFF, size);
//				A[register]++;
//			} else if (size == Size.WORD) {
//				bus.write(addr, data & 0xFFFF, size);
//				A[register] += 2;
//			} else if (size == Size.LONG) {
//				bus.write(addr, data >> 16, size);
//				bus.write(addr + 2, data & 0xFFFF, size);
//				A[register] += 4;
//			}
//		} else if (mode == 0b100) {		//	-(An)
//			long address = A[register];
//			
//			if (size == Size.WORD) {
//				address = (address - 2) & 0xFFFF_FFFFL;
//				A[register] = address;	// predecrement
//				
//				bus.write(address, data, size);
//			} else if (size == Size.LONG) {
//				address = (address - 4) & 0xFFFF_FFFFL;
//				A[register] = address;	// predecrement
//				
//				bus.write(address, (data >> 16) & 0xFFFF, size);
//				bus.write(address + 2, (data & 0xFFFF), size);
//				
//			} else {
//				throw new RuntimeException("NOT " + size);
//			}
//			
//		} else if (mode == 0b101) {		//	(d16,An)	Address with Displacement
//			if (size == Size.BYTE) {	//	byte
//				long base = A[register];
//				long displac = (bus.read(offset) << 8);
//				displac 	|= (bus.read(offset + 1));
//				
//				if ((displac & 0x80) > 0) {
//					displac |= 0xFFFF_FF00L;	// sign extend 32 bits
//				}
//				addr = (base + displac);
//				bus.write(addr, data & 0xFF, size);
//				
//			} else if (size == Size.WORD) {
//				long base = A[register];
//				long displac = (bus.read(offset) << 8);
//				displac 	|= (bus.read(offset + 1));
//				
//				if ((displac & 0x8000) > 0) {
//					displac |= 0xFFFF_0000L;	// sign extend 32 bits
//				}
//				addr = (base + displac);
//				bus.write(addr, data & 0xFFFF, size);
//				
//			} else if (size == Size.LONG) {
//				long base = A[register];
//				long displac = (bus.read(offset) << 8);
//				displac 	|= (bus.read(offset + 1));
//				
//				//	tiene sign extend esto ?
//				
//				addr = (base + displac);
//				bus.write(addr, (data >> 16), Size.WORD);		// FIXME, manejar write como long word y arreglar todo
//				bus.write(addr + 2, (data & 0xFFFF), Size.WORD);
//			}
//			
//			PC += 2;
//		
//		} else if (mode == 0b110) {		//	 Address Register Indirect with Index (Base Displacement) Mode
//			long exten  = (bus.read(PC + 2) << 8);
//		         exten |= (bus.read(PC + 3));
//			int displacement = (int) (exten & 0xFF);		// es 8 bits, siempre el ultimo byte ?
//			
//			if ((displacement & 0x80) > 0) { 	// sign extend
//				displacement = 0xFFFF_FF00 | displacement;
//			}
//			int idxRegNumber = (int) ((exten >> 12) & 0x07);
//			Size idxSize = ((exten & 0x0800) == 0x0800 ? Size.LONG : Size.WORD);
//			boolean idxIsAddressReg = ((exten & 0x8000) == 0x8000);
//			long idxVal;
//			if (idxIsAddressReg) {
//				if (idxSize == Size.WORD) {
//					idxVal = getA(idxRegNumber);
//					if ((data & 0x8000) > 0) {
//						idxVal = 0xFFFF_0000 | idxVal;
//					}
//				} else {
//					idxVal = getA(idxRegNumber);
//				}
//			} else {
//				if (idxSize == Size.WORD) {
//					idxVal = getD(idxRegNumber);
//					if ((data & 0x8000) > 0) {
//						idxVal = 0xFFFF_0000 | idxVal;
//					}
//				} else {
//					idxVal = getD(idxRegNumber);
//				}
//			}
//			
//			long address = getA(register) + displacement + idxVal;
//			if (size == Size.BYTE) {
//				bus.write(address, data, size);
//			} else if (size == Size.WORD) {
//				bus.write(address, data, size);
//			} else if (size == Size.LONG) {
//				bus.write(address, data, size);
//			}
//			
//			PC += 2;
//			
//		} else if (mode == 0b111) {
//			if (register == 0b000) {			//	Abs.W
//				addr  = (bus.read(offset) << 8);
//				addr |= (bus.read(offset + 1) << 0);
//			
//				if ((addr & 0x8000) > 0) {
//					addr |= 0xFFFF_0000;
//				}
//				
//				if (size == Size.BYTE) {
//					bus.write(addr, data, size);
//				} else if (size == Size.WORD) {
//					bus.write(addr, data, size);
//				} else if (size == Size.LONG) {
//					bus.write(addr, (data >> 16), size);
//					bus.write(addr + 2, data & 0xFFFF, size);
//				}
//				
//				PC += 2;
//				
//			} else if (register == 0b001) {		//	Abs.L
//				addr  = (bus.read(offset) << 24);
//				addr |= (bus.read(offset + 1) << 16);
//				addr |= (bus.read(offset + 2) << 8);
//				addr |= (bus.read(offset + 3) << 0);
//			
//				if (size == Size.BYTE) {
//					bus.write(addr, data & 0xFF, size);
//					
//				} else if (size == Size.WORD) {
//					bus.write(addr, data & 0xFFFF, size);
//					
//				} else if (size == Size.LONG) {
//					bus.write(addr, data >> 16, size);
//					bus.write(addr + 2, data & 0xFFFF, size);
//					
//				}
//				
//				PC += 4;
//				
//			} else if (register == 0b010) {		//	(d16,PC)	Program Counter Indirect with Displacement Mode
//				throw new RuntimeException("IMPL");
//			} else if (register == 0b100) {		//	#data
//				throw new RuntimeException("IMPL");
//			} else {
//				throw new RuntimeException("Addressing no soportado: " + mode + " REG: " + register);
//			}
//		} else {
//			throw new RuntimeException("Addressing no soportado: " + mode);
//		}
//	}
	
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

//	Condition code 'cc' specifies one of the following:
//0000 F  False            Z = 1      1000 VC oVerflow Clear   V = 0
//0001 T  True             Z = 0      1001 VS oVerflow Set     V = 1
//0010 HI HIgh             C + Z = 0  1010 PL PLus             N = 0
//0011 LS Low or Same      C + Z = 1  1011 MI MInus            N = 1
//0100 CC Carry Clear      C = 0      1100 GE Greater or Equal N (+) V = 0
//0101 CS Carry Set        C = 1      1101 LT Less Than        N (+) V = 1
//0110 NE Not Equal        Z = 0      1110 GT Greater Than     Z + (N (+) V) = 0
//0111 EQ EQual            Z = 1      1111 LE Less or Equal    Z + (N (+) V) = 1
	public boolean evaluateBranchCondition(int cc, Size size) {
		boolean taken;
		
		switch (cc) {
		case 0b0000:
			taken = true;
			break;
		case 0b0001:
			// es un BSR
			long oldPC;
			if (size == Size.BYTE) {
				oldPC = PC + 2;
			} else if (size == Size.WORD) {
				oldPC = PC + 4;
			} else {
				throw new RuntimeException("");
			}
			
			taken = true;
			
			SSP--;
			bus.write(SSP, oldPC & 0xFF, Size.BYTE);
			SSP--;
			bus.write(SSP, (oldPC >> 8) & 0xFF, Size.BYTE);
			SSP--;
			bus.write(SSP, (oldPC >> 16) & 0xFF, Size.BYTE);
			SSP--;
			bus.write(SSP, (oldPC >> 24), Size.BYTE);
			
			setALong(7, SSP);
			
			break;
		case 0b0010:	//	C + Z = 0		the C and Z flags are both clear
			taken = !isC() && !isZ();
			break;
		case 0b0011:	//	C + Z = 1		the C or Z flag is set
			taken = isC() || isZ();
			break;
		case 0b0100:
			taken = !isC();
			break;
		case 0b0101:
			taken = isC();
			break;
		case 0b0110:
			taken = !isZ();
			break;
		case 0b0111:
			taken = isZ();
			break;
		case 0b1000:
			taken = !isV();
			break;
		case 0b1001:
			taken = isV();
			break;
		case 0b1010:
			taken = !isN();
			break;
		case 0b1011:
			taken = isN();
			break;
		case 0b1100:	//	BGE – Branch on Greater than or Equal	1) The N and V flags are both clear 2) The N and V flags are both set
			taken = (!isN() && !isV()) || (isN() && isV());
			break;
		case 0b1101:	//	BLT – Branch on Lower Than	N (+) V = 1		1) The N flag is clear, but the V flag is set 2) The N flag is set, but the V flag is clear
			taken = (!isN() && isV()) || (isN() && !isV());
			break;
		case 0b1110:	//	BGT Greater Than     Z + (N (+) V) = 0		1) The Z, N and V flags are all clear 2) The Z flag is clear, but the N and V flags are both set
			taken = (!isZ() && !isN() && !isV()) || (!isZ() && isN() && isV());
			break;
		case 0b1111:	//	BLE Less or Equal    Z + (N (+) V) = 1		1) The Z flag is clear 2) The N flag is clear, but the V flag is set 3) The N flag is set, but the V flag is clear
			taken = isZ() || (!isZ() && !isN() && isV()) || (!isZ() && isN() && !isV());
			break;
			default:
				throw new RuntimeException("not impl " + cc);
		}
		
		return taken;
	}
	
}
