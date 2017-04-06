package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class MOVE_FROM_SR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public MOVE_FROM_SR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	MOVE from SR -- Move from status register (privileged)
//
//SYNOPSIS
//	MOVE	SR,<ea>
//
//	Size = (Word)
//
//FUNCTION
//	The content of the status register is moved to the destination
//	location. The operand size is a word.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 1 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                  <ea>
//
//REGISTER
//	<ea> specifies destination operand, addressing modes allowed are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |    -     | |    Abs.L      |111 |  001   |
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
	
	@Override
	public void generate() {
		int base = 0x40C0;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				MOVEFromSR(opcode);
			}
		};

		for (int m = 0; m < 8; m++) {
			for (int r = 0; r < 8; r++) {
				int opcode = base + (m << 3) | (r);
				cpu.addInstruction(opcode, ins);
			}
		}
	}
	
	private void MOVEFromSR(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;

		long data = cpu.SR;
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);	//TODO es escritura, el mode immediate se tranforma por otro, hacer nuevo metodo !!
		o.setData(data);
		o.getAddressingMode().setWord(o);
	}
	
}
