package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ADDA implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ADDA(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	ADDA -- Add address
//
//SYNOPSIS
//	ADDA	<ea>,An
//
//	Size = (Word, Long)
//
//FUNCTION
//	Adds the source operand to the destination address register,
//	and stores the result in the destination address register. The size
//	of the operation may be specified as word or long. The entire
//	destination operand is used regardless of the operation size.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|-----------|-----------|-----------|
//	| 1 | 1 | 0 | 1 |  REGISTER |  OP-MODE  |    MODE   |  REGISTER |
//	----------------------------------------=========================
//                                                          <ea>
//OP-MODE
//	Indicates operation lenght:
//	011->one Word operation: source operand is extended to 32 bits
//	111->one Long operation
//
//REGISTER
//	One of the 8 address registers.
//	<ea> is always source, all addressing modes are allowed.
//
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       |001 |N° reg. An| |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d16,An)    |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   | 
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      |111 |  100   |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//	RESULT
//	None.
	
	@Override
	public void generate() {
		int base = 0xD000;
		GenInstruction ins = null;
		int opMode = 0;
		
		for (int s = 0; s < 2; s++) {
			if (s == 0) {
				opMode = 0b011;
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADDAWord(opcode);
					}
				};
			} else if (s == 1) {
				opMode = 0b111;
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADDALong(opcode);
					}
				};
			}
			
			for (int register = 0; register < 8; register++) {
				for (int m = 0; m < 8; m++) {
					for (int r = 0; r < 8; r++) {
						if (m == 0b111 & r > 0b100) {
							continue;
						}
						int opcode = base | (register << 9) | (opMode << 6) | (m << 3) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
		
	}
	
	private void ADDAWord(int opcode) {
		int addrRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		if ((data & 0x8000) > 0) {
			data = 0xFFFF_0000 | data;
		}
		
		long toAdd = cpu.getALong(addrRegister);
		long tot = (toAdd + data);
		cpu.setALong(addrRegister, tot);	// setLong porque afecta a todo el registro
	}
	
	private void ADDALong(int opcode) {
		int addrRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long toAdd = cpu.getALong(addrRegister);
		long tot = (toAdd + data);
		cpu.setALong(addrRegister, tot);
	}
	
}
