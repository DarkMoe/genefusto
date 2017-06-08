package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ABCD implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ABCD(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	ABCD -- Add binary coded decimal
//
//SYNOPSIS
//	ABCD	Dy,Dx
//	ABCD	-(Ay),-(Ax)
//
//	Size = (Byte)
//
//FUNCTION
//	Adds the source operand to the destination operand along with
//	the extend bit, and stores the result in the destination location.
//	The addition is performed using binary coded decimal arithmetic.
//	The operands, which are packed BCD numbers, can be addressed in
//	two different ways:
//
//	1. Data register to data register: The operands are contained in the
//	   data registers specified in the instruction.
//
//	2. Memory to memory: The operands are addressed with the predecrement
//	   addressing mode using the address registers specified in the
//	   instruction.
//
//	This operation is a byte operation only.
//
//	Normally the Z condition code bit is set via programming before the
//	start of an operation. That allows successful tests for zero results
//	upon completion of multiple-precision operations.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|---|---|---|---|---|-----------|
//	| 1 | 1 | 0 | 0 |    Rx     | 1 | 0 | 0 | 0 | 0 |R/M|    Ry     |
//	-----------------------------------------------------------------
//
//	R/M = 0 -> data register
//	R/M = 1 -> address register
//	Rx:   destination register
//	Ry:   source register
//
//RESULT
//	X - Set the same as the carry bit.
//	N - Undefined
//	Z - Cleared if the result is non-zero. Unchanged otherwise.
//	V - Undefined
//	C - Set if a decimal carry was generated. Cleared otherwise.

	@Override
	public void generate() {
		generateDataOperation();
		generateAddressOperation();
	}
	
	private void generateDataOperation() {
		int base = 0xC100;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ABCDDataByte(opcode);
			}
		};
				
		for (int rx = 0; rx < 8; rx++) {
			for (int ry = 0; ry < 8; ry++) {
				int opcode = base | (rx << 9) | ry;
				cpu.addInstruction(opcode, ins);
			}
		}
	}
	
	private void generateAddressOperation() {
		int base = 0xC108;
		GenInstruction ins = null;

		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ABCDAddressByte(opcode);
			}
		};
			
		for (int rx = 0; rx < 8; rx++) {
			for (int ry = 0; ry < 8; ry++) {
				int opcode = base | (rx << 9) | ry;
				cpu.addInstruction(opcode, ins);
			}
		}
	}

	private void ABCDDataByte(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long data = cpu.getDByte(ry);
		long toAdd = cpu.getDByte(rx);
		
		long tot = doCalc(data, toAdd);
		cpu.setDByte(rx, tot);
	}
	
	private void ABCDAddressByte(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long source = cpu.getALong(ry);
		long dest = cpu.getALong(rx);
		
		source--;
		dest--;
		
		cpu.setALong(ry, source);
		cpu.setALong(rx, dest);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, 0b010, ry);	//	address indirect
		long data = o.getAddressingMode().getByte(o);
		
		Operation o2 = cpu.resolveAddressingMode(Size.BYTE, 0b010, rx);	//	address indirect
		long toAdd = o2.getAddressingMode().getByte(o2);
		
		long tot = doCalc(data, toAdd);
		cpu.writeKnownAddressingMode(o2, tot, Size.BYTE);
	}
	
	protected final long doCalc(long data, long toAdd) {
		int x = (cpu.isX() ? 1 : 0);
		int c;

		long lo = (data & 0x0F) + (toAdd & 0x0F) + x;
		if (lo > 9) {
			lo -= 10;
			c = 1;
		} else {
			c = 0;
		}

		long hi = ((data >> 4) & 0x0F) + ((toAdd >> 4) & 0x0F) + c;
		if (hi > 9) {
			hi -= 10;
			c = 1;
		} else {
			c = 0;
		}

		long result = (hi << 4) + lo;

		if (c != 0) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}

		if (result != 0) {
			cpu.clearZ();
		}

		return result;
	}
	
}
