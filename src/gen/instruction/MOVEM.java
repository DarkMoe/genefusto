package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;
import gen.addressing.AbsoluteLong;
import gen.addressing.AbsoluteShort;
import gen.addressing.AddressRegisterIndirect;
import gen.addressing.AddressRegisterIndirectPostIncrement;
import gen.addressing.AddressRegisterIndirectPreDecrement;
import gen.addressing.AddressRegisterWithDisplacement;
import gen.addressing.AddressRegisterWithIndex;
import gen.addressing.PCWithDisplacement;
import gen.addressing.PCWithIndex;

public class MOVEM implements GenInstructionHandler {

	final Gen68 cpu;
	
	public MOVEM(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	MOVEM -- Move multiple registers
//
//SYNOPSIS
//	MOVEM	<register list>,<ea>
//	MOVEM	<ea>,<register list>
//
//	Size = (Word, Long)
//
//FUNCTION
//	Registers in the register list are either moved to or
//	fetched from consecutive memory locations at the specified
//	address. Data can be either word or long word, but if
//	the register list is destination and the size is word,
//	each register is filled with the source word sign extended
//	to 32-bits.
//
//	Also, in the case that the register list is the destination,
//	register indirect with predecrement is not a valid source
//	mode. If the register list is the source, then the
//	destination may not be register indirect with postincrement.
//
//		MOVEM.L	D0/D1/A0,(A2)+		;invalid
//		MOVEM.W	-(A1),D5/D7/A4		;invalid
//
//	The register list is accessed with D0 first through D7, followed
//	by A0 through A7.
//
//FORMAT
//	                                                  <ea>
//	-----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6  | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|----|-----------|-----------|
//	| 0 | 1 | 0 | 0 | 1 |dr | 0 | 0 | 1 |SIZE|    MODE   | REGISTER  |
//	|----------------------------------------------------------------|
//	|                    MASK FROM REGISTER LIST                     |
//	------------------------------------------------------------------
//
//	dr specifies move direction:
//	0->registers to memory
//	1->memory to registers
//
//	MASK FROM REGISTER LIST specifies registers which must be moved and
//	indicates their move order.
//
//	For pre-decrementing, mask has the following format:
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	|D0 |D1 |D2 |D3 |D4 |D5 |D6 |D7 |A0 |A1 |A2 |A3 |A4 |A5 |A6 |A7 |
//	------------------------------------------------------------=====
//	                                                              |
//	First register to be moved------------------------------------'
//
//	For post-incrementing, mask has the following format:
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	|A7 |A6 |A5 |A4 |A3 |A2 |A1 |A0 |D7 |D6 |D5 |D4 |D3 |D2 |D1 |D0 |
//	------------------------------------------------------------=====
//	                                                              |
//	First register to be moved------------------------------------'
//
//SIZE
//	0->16 bits.
//	1->32 bits.
//
//REGISTER
//	<ea> specifies memory address of move.
//
//	Move from registers to memory, addressing modes allowed are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       | -  |    -     | |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |    -     | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     | -  |    -     | |   (d8,PC,Xi)  | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)| -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)| -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      | -  |   -    |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//
//	Move from memory to registers, addressing modes allowed are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       | -  |    -     | |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |    -     | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     | -  |    -     | |   (d8,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      | -  |   -    |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//
//RESULT
//	None.

	@Override
	public void generate() {
		int base = 0x4880;
		GenInstruction ins = null;
		
		for (int dr = 0; dr < 2; dr++) {
			if (dr == 0) {
				for (int s = 0; s < 2; s++) {
					if (s == 0) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								MOVEMRegsToMemWord(opcode);
							}
						};
					} else if (s == 1) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								MOVEMRegsToMemLong(opcode);
							}
						};
					}
					
					for (int m = 0; m < 8; m++) {
						if (m == 0b000 || m == 0b001 || m == 0b011) {
							continue;
						}
						
						for (int r = 0; r < 8; r++) {
							if ((m == 0b111) && r > 0b011) {
								continue;
							}
							
							int opcode = base | (dr << 10) | (s << 6) | (m << 3) | r;
							cpu.addInstruction(opcode, ins);
						}
					}
				}
			} else if (dr == 1) {
				for (int s = 0; s < 2; s++) {
					if (s == 0) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								MOVEMMemToRegsWord(opcode);
							}
						};
					} else if (s == 1) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								MOVEMMemToRegsLong(opcode);
							}
						};
					}
					
					for (int m = 0; m < 8; m++) {
						if (m == 0b000 || m == 0b001 || m == 0b100) {
							continue;
						}
						
						for (int r = 0; r < 8; r++) {
							if ((m == 0b111) && r > 0b011) {
								continue;
							}
							
							int opcode = base | (dr << 10) | (s << 6) | (m << 3) | r;
							cpu.addInstruction(opcode, ins);
						}
					}
				}
			}
		}
	}
	
	private void MOVEMMemToRegsWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		long data;
		
		int registerListMaskA = (int) cpu.bus.read(cpu.PC + 2, Size.BYTE);	// TODO ojo q con pre decrement es al reves la interpretacion
		int registerListMaskD = (int) cpu.bus.read(cpu.PC + 3, Size.BYTE);

		cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		for (int i = 0; i < 8; i++) {
			if (((registerListMaskD) & (1 << i)) != 0) {
				data = o.getAddressingMode().getWord(o);
				
				if ((data & 0x8000) > 0) {	//	sign extend para registros destino y size word
					data |= 0xFFFF_0000L;
				}
				
				cpu.setDLong(i, data);
				
				if (o.getAddressingMode() instanceof AddressRegisterIndirectPostIncrement) {
					o.setAddress(o.getAddress() + 2);
					cpu.setAWord(register, o.getAddress());
				} else if (o.getAddressingMode() instanceof AbsoluteShort
						|| o.getAddressingMode() instanceof AbsoluteLong
						|| o.getAddressingMode() instanceof AddressRegisterIndirect
						|| o.getAddressingMode() instanceof AddressRegisterWithDisplacement
						|| o.getAddressingMode() instanceof AddressRegisterWithIndex
						|| o.getAddressingMode() instanceof PCWithDisplacement
						|| o.getAddressingMode() instanceof PCWithIndex) {
					o.setAddress(o.getAddress() + 2);
				} else {
					throw new RuntimeException(o.getAddressingMode().getClass().getSimpleName());
				}
			}
		}
		for (int i = 0; i < 8; i++) {
			if (((registerListMaskA) & (1 << i)) != 0) {
				data = o.getAddressingMode().getWord(o);
				
				if ((data & 0x8000) > 0) {	//	sign extend para registros destino y size word
					data |= 0xFFFF_0000L;
				}
				
				cpu.setALong(i, data);
				
				if (o.getAddressingMode() instanceof AddressRegisterIndirectPostIncrement) {
					o.setAddress(o.getAddress() + 2);
					cpu.setAWord(register, o.getAddress());
				} else if (o.getAddressingMode() instanceof AbsoluteShort
						|| o.getAddressingMode() instanceof AbsoluteLong
						|| o.getAddressingMode() instanceof AddressRegisterIndirect
						|| o.getAddressingMode() instanceof AddressRegisterWithDisplacement
						|| o.getAddressingMode() instanceof AddressRegisterWithIndex
						|| o.getAddressingMode() instanceof PCWithDisplacement
						|| o.getAddressingMode() instanceof PCWithIndex) {
					o.setAddress(o.getAddress() + 2);
				} else {
					throw new RuntimeException("D");
				}
			}
		}
	}
	
	private void MOVEMMemToRegsLong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		long data;
		
		int registerListMaskA = (int) cpu.bus.read(cpu.PC + 2, Size.BYTE);
		int registerListMaskD = (int) cpu.bus.read(cpu.PC + 3, Size.BYTE);

		cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		
		for (int i = 0; i < 8; i++) {
			if (((registerListMaskD) & (1 << i)) != 0) {
				data = o.getAddressingMode().getLong(o);
				
				cpu.setDLong(i, data);
				
				if (o.getAddressingMode() instanceof AddressRegisterIndirectPostIncrement) {
					o.setAddress(o.getAddress() + 4);
					cpu.setALong(register, o.getAddress());
				} else if (o.getAddressingMode() instanceof AbsoluteShort
						|| o.getAddressingMode() instanceof AbsoluteLong
						|| o.getAddressingMode() instanceof AddressRegisterIndirect
						|| o.getAddressingMode() instanceof AddressRegisterWithDisplacement) {
					o.setAddress(o.getAddress() + 4);
				} else {
					throw new RuntimeException("A");
				}
			}
		}
		for (int i = 0; i < 8; i++) {
			if (((registerListMaskA) & (1 << i)) != 0) {
				data = o.getAddressingMode().getLong(o);
				
				cpu.setALong(i, data);
				
				if (o.getAddressingMode() instanceof AddressRegisterIndirectPostIncrement) {
					o.setAddress(o.getAddress() + 4);
					cpu.setALong(register, o.getAddress());
				} else if (o.getAddressingMode() instanceof AbsoluteShort
						|| o.getAddressingMode() instanceof AbsoluteLong
						|| o.getAddressingMode() instanceof AddressRegisterIndirect
						|| o.getAddressingMode() instanceof AddressRegisterWithDisplacement) {
					o.setAddress(o.getAddress() + 4);
				} else {
					throw new RuntimeException(o.getAddressingMode().getClass().getSimpleName());
				}
			}
		}
	}
	
	private void MOVEMRegsToMemWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		long data;
		
		int msb = (int) cpu.bus.read(cpu.PC + 2, Size.BYTE);
		int lsb = (int) cpu.bus.read(cpu.PC + 3, Size.BYTE);
		
		cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
	
		int registerListMaskD = 0;
		int registerListMaskA = 0;
		if (o.getAddressingMode() instanceof AddressRegisterIndirectPreDecrement) {
			for (int i = 0; i < 8; i++) {
				registerListMaskD <<= 1;
				registerListMaskD |= (msb & 1);
				msb >>= 1;
			}
			for (int i = 0; i < 8; i++) {
				registerListMaskA <<= 1;
				registerListMaskA |= (lsb & 1);
				lsb >>= 1;
			}
			
			for (int i = 7; i >= 0; i--) {
				if (((registerListMaskA) & (1 << i)) != 0) {
					data = cpu.getAWord(i);
					
					if (i == 7) {
						throw new RuntimeException("CASO EDGE !!!! IMPL");
					}
					
					cpu.writeKnownAddressingMode(o, data, Size.WORD);
					
					o.setAddress(o.getAddress() - 2);
				}
			}
			for (int i = 7; i >= 0; i--) {
				if (((registerListMaskD) & (1 << i)) != 0) {
					data = cpu.getDWord(i);
					
					cpu.writeKnownAddressingMode(o, data, Size.WORD);
					
					o.setAddress(o.getAddress() - 2);
				}
			}
			if (registerListMaskD != 0 || registerListMaskA != 0) {
				o.setAddress(o.getAddress() + 2);
			}
			
			cpu.setAWord(register, o.getAddress() & 0xFFFF);
			
		} else {
			registerListMaskA = msb;
			registerListMaskD = lsb;
			
			for (int i = 0; i < 8; i++) {
				if (((registerListMaskD) & (1 << i)) != 0) {
					data = cpu.getDWord(i);
					
					cpu.writeKnownAddressingMode(o, data, Size.WORD);
					
					o.setAddress(o.getAddress() + 2);
				}
			}
			for (int i = 0; i < 8; i++) {
				if (((registerListMaskA) & (1 << i)) != 0) {
					data = cpu.getAWord(i);
					
					cpu.writeKnownAddressingMode(o, data, Size.WORD);
					
					o.setAddress(o.getAddress() + 2);
				}
			}
		}
		
	}
	
	private void MOVEMRegsToMemLong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		long data;
		
		int msb = (int) cpu.bus.read(cpu.PC + 2, Size.BYTE);
		int lsb = (int) cpu.bus.read(cpu.PC + 3, Size.BYTE);
		
		cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
	
		int registerListMaskD = 0;
		int registerListMaskA = 0;
		if (o.getAddressingMode() instanceof AddressRegisterIndirectPreDecrement) {
			for (int i = 0; i < 8; i++) {
				registerListMaskD <<= 1;
				registerListMaskD |= (msb & 1);
				msb >>= 1;
			}
			for (int i = 0; i < 8; i++) {
				registerListMaskA <<= 1;
				registerListMaskA |= (lsb & 1);
				lsb >>= 1;
			}
			
			for (int i = 7; i >= 0; i--) {
				if (((registerListMaskA) & (1 << i)) != 0) {
					data = cpu.getALong(i);
					
					if (i == 7) {
						throw new RuntimeException("CASO EDGE !!!! IMPL");
					}
					
					cpu.writeKnownAddressingMode(o, data, Size.LONG);
					
					o.setAddress(o.getAddress() - 4);
				}
			}
			for (int i = 7; i >= 0; i--) {
				if (((registerListMaskD) & (1 << i)) != 0) {
					data = cpu.getDLong(i);
					
					cpu.writeKnownAddressingMode(o, data, Size.LONG);
					
					o.setAddress(o.getAddress() - 4);
				}
			}
			if (registerListMaskD != 0 || registerListMaskA != 0) {
				o.setAddress(o.getAddress() + 4);
			}
			
			cpu.setALong(register, o.getAddress());
			
		} else {
			registerListMaskA = msb;
			registerListMaskD = lsb;
			
			for (int i = 0; i < 8; i++) {
				if (((registerListMaskD) & (1 << i)) != 0) {
					data = cpu.getDLong(i);
					
					cpu.writeKnownAddressingMode(o, data, Size.LONG);
					
					o.setAddress(o.getAddress() + 4);
				}
			}
			for (int i = 0; i < 8; i++) {
				if (((registerListMaskA) & (1 << i)) != 0) {
					data = cpu.getALong(i);
					
					cpu.writeKnownAddressingMode(o, data, Size.LONG);
					
					o.setAddress(o.getAddress() + 4);
				}
			}
		}
		
	}
	
}
