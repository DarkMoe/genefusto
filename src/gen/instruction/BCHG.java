package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class BCHG implements GenInstructionHandler {

	final Gen68 cpu;
	
	public BCHG(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	BCHG -- Bit change
//
//SYNOPSIS
//	BCHG	Dn,<ea>
//	BCHG	#<data>,<ea>
//
//	Size = (Byte, Long)
//
//FUNCTION
//	Tests a bit in the destination operand and sets the Z condition
//	code appropriately, then inverts the bit in the destination.
//	If the destination is a data register, any of the 32 bits can be
//	specified by the modulo 32 number. When the destination is a memory
//	location, the operation must be a byte operation, and therefore the
//	bit number is modulo 8. In all cases, bit zero is the least
//	significant bit. The bit number for this operation may be specified
//	in either of two ways:
//
//	1. Immediate -- The bit number is specified as immediate data.
//	2. Register  -- The specified data register contains the bit number.
//
//FORMAT
//	In the case of BCHG Dn,<ea>:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|---|---|-----------|-----------|
//	| 0 | 0 | 0 | 0 |  REGISTER | 1 | 0 | 1 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                  <ea>
//
//	In the case of BCHG #<data,<ea>:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	                                                  <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 |    MODE   | REGISTER  |
//	|---|---|---|---|---|---|---|---|-------------------------------|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |       NUMBER OF THE BIT       |
//	-----------------------------------------------------------------
//
//REGISTER
//	<ea> is always destination, addressing modes are the followings:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn *     |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |     -    | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  | -  |   -    |
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
//	 * Long only; for others modes: Byte only.
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
		int base = 0x0140;
		GenInstruction insByte = new GenInstruction() {
			@Override
			public void run(int opcode) {
				BCHGRegisterByte(opcode);
			}
		};
		GenInstruction insLong = new GenInstruction() {
			@Override
			public void run(int opcode) {
				BCHGRegisterLong(opcode);
			}
		};
		
		for (int m = 0; m < 8; m++) {
			if (m == 1) {
				continue;
			}
			
			for (int r = 0; r < 8; r++) {
				if ((m == 0b111) && r > 0b001) {
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

	private void generateImmediate(Gen68 cpu) {
		int base = 0x0840;
		GenInstruction insByte = new GenInstruction() {
			@Override
			public void run(int opcode) {
				BCHGImmediateByte(opcode);
			}
		};
		GenInstruction insLong = new GenInstruction() {
			@Override
			public void run(int opcode) {
				BCHGImmediateLong(opcode);
			}
		};
		
		for (int m = 0; m < 8; m++) {
			if (m == 1) {
				continue;
			}
			
			for (int r = 0; r < 8; r++) {
				if ((m == 0b111) && r > 0b001) {
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

	private void BCHGImmediateByte(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		
		long numberBit = cpu.bus.read(cpu.PC + 2, Size.WORD);
		numberBit &= 7;
		
		cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		calcFlags(data, (int) numberBit);
	
		if (cpu.bitTest((int) data, (int) numberBit)) {
			data = cpu.bitReset((int) data, (int) numberBit);
		} else {
			data = cpu.bitSet((int) data, (int) numberBit);
		}
		o.setData(data);
		
		cpu.writeKnownAddressingMode(o, data, Size.BYTE);
	}
	
	private void BCHGImmediateLong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		
		long numberBit = cpu.bus.read(cpu.PC + 2, Size.WORD);
		numberBit &= 31;
		
		cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		calcFlags(data, (int) numberBit);
	
		if (cpu.bitTest((int) data, (int) numberBit)) {
			data = cpu.bitReset((int) data, (int) numberBit);
		} else {
			data = cpu.bitSet((int) data, (int) numberBit);
		}
		o.setData(data);
		
		cpu.writeKnownAddressingMode(o, data, Size.LONG);
	}
	
	private void BCHGRegisterByte(int opcode) {
		throw new RuntimeException();
	}
	
	private void BCHGRegisterLong(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		
		long numberBit = cpu.getDLong(dataRegister);
		numberBit &= 31;
		
		cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		calcFlags(data, (int) numberBit);
	
		if (cpu.bitTest((int) data, (int) numberBit)) {
			data = cpu.bitReset((int) data, (int) numberBit);
		} else {
			data = cpu.bitSet((int) data, (int) numberBit);
		}
		o.setData(data);
		
		cpu.writeKnownAddressingMode(o, data, Size.LONG);
	}
	
	void calcFlags(long data, int bit) {
		if (cpu.bitTest(data, bit)) {
			cpu.clearZ();
		} else {
			cpu.setZ();
		}
	}
	
}
