package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class BTST implements GenInstructionHandler {

	final Gen68 cpu;
	
	public BTST(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	BTST -- Bit test
//
//SYNOPSIS
//	BTST	Dn,<ea>
//	BTST	#<data>,<ea>
//
//	Size = (Byte, Long)
//
//FUNCTION
//	Tests a bit in the destination operand and sets the Z
//	condition code appropriately. If the destination is a data register,
//	any of the 32 bits can be specified by the modulo 32 number. When
//	the distination is a memory location, the operation must be a byte
//	operation, and therefore the bit number is modulo 8. In all cases,
//	bit zero is the least significant bit. The bit number for this
//	operation may be specified in either of two ways:
//
//	1. Immediate -- The bit number is specified as immediate data.
//	2. Register  -- The specified data register contains the bit number.
//
//FORMAT
//	In the case of BTST Dn,<ea>:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|---|---|-----------|-----------|
//	| 0 | 0 | 0 | 0 |  REGISTER | 1 | 0 | 0 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                  <ea>
//
//	In the case of BTST #<data,<ea>:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	                                                  <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 |    MODE   | REGISTER  |
//	|---|---|---|---|---|---|---|---|-------------------------------|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |       NUMBER OF THE BIT       |
//	-----------------------------------------------------------------
//
//REGISTER
//	In the case of BTST Dn,<ea>:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	<ea> is always destination, addressing modes are the followings:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn *     |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |     -    | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      |111 |  100   |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//	 * Long only; for others modes: Byte only.
//
//	In the case of BTST #<data,<ea>:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn *     |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |     -    | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  |111 |  011   |
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
//	X - not affected
//	N - not affected
//	Z - Set if the bit tested is zero. Cleared otherwise.
//	V - not affected
//	C - not affected
	
	@Override
	public void generate() {
		generateRegister(cpu);
		generateImmediate(cpu);
	}
	
	private void generateRegister(Gen68 cpu) {
		int base = 0x0100;
		GenInstruction insByte = new GenInstruction() {
			@Override
			public void run(int opcode) {
				BTSTRegisterByte(opcode);
			}
		};
		GenInstruction insLong = new GenInstruction() {
			@Override
			public void run(int opcode) {
				BTSTRegisterLong(opcode);
			}
		};
		
		for (int register = 0; register < 8; register++) {
			for (int m = 0; m < 8; m++) {
				if (m == 1) {
					continue;
				}
				for (int r = 0; r < 8; r++) {
					if ((m == 7) && r > 0b100) {
						continue;
					}
		
					int opcode = base | (register << 9) | (m << 3) | r;
					if (m == 0) {
						cpu.addInstruction(opcode, insLong);	// only long
					} else {
						cpu.addInstruction(opcode, insByte);
					}
					
				}
			}
		}
	}

	private void generateImmediate(Gen68 cpu) {
		int base = 0x0800;
		GenInstruction insByte = new GenInstruction() {
			@Override
			public void run(int opcode) {
				BTSTImmediateByte(opcode);
			}
		};
		GenInstruction insLong = new GenInstruction() {
			@Override
			public void run(int opcode) {
				BTSTImmediateLong(opcode);
			}
		};
		
		for (int m = 0; m < 8; m++) {
			if (m == 1) {
				continue;
			}
			
			for (int r = 0; r < 8; r++) {
				if ((m == 7) && r > 0b011) {
					continue;
				}
				
				int opcode = base | (m << 3) | r;
				if (m == 0) {
					cpu.addInstruction(opcode, insLong);
				} else {
					cpu.addInstruction(opcode, insByte);
				}
			}
		}		
	}

	private void BTSTRegisterByte(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int destMode = (opcode >> 3) & 0x7;
		int destReg = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, destMode, destReg);
		long data = o.getAddressingMode().getByte(o);
		
		long bitNumber = cpu.getDByte(dataRegister);
		bitNumber &= 7;
		
		calcFlags(data, (int) bitNumber);
	}
	
	private void BTSTRegisterLong(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int destMode = (opcode >> 3) & 0x7;
		int destReg = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, destMode, destReg);
		long data = o.getAddressingMode().getLong(o);
		
		long bitNumber = cpu.getDLong(dataRegister);
		bitNumber &= 31;
		
		calcFlags(data, (int) bitNumber);
	}
	
	private void BTSTImmediateByte(int opcode) {
		int destReg = (opcode & 0x7);
		int destMode = (opcode >> 3) & 0x7;
		
		long bitNumber = cpu.bus.read(cpu.PC + 2, Size.WORD);
		bitNumber = bitNumber & 0xFF;
		bitNumber &= 7;
		
		cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, destMode, destReg);
		long data = o.getAddressingMode().getByte(o);
		
		calcFlags(data, (int) bitNumber);
	}
	
	private void BTSTImmediateLong(int opcode) {
		int destReg = (opcode & 0x7);
		int destMode = (opcode >> 3) & 0x7;
		
		long bitNumber = cpu.bus.read(cpu.PC + 2, Size.WORD);
		bitNumber = bitNumber & 0xFF;
		bitNumber &= 31;
		
		cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, destMode, destReg);
		long data = o.getAddressingMode().getLong(o);
		
		calcFlags(data, (int) bitNumber);
	}

	void calcFlags(long data, int bit) {
		if (cpu.bitTest(data, bit)) {
			cpu.clearZ();
		} else {
			cpu.setZ();
		}
	}
	
}
