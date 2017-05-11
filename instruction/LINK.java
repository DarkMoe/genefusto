package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class LINK implements GenInstructionHandler {

	final Gen68 cpu;
	
	public LINK(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	LINK -- Create local stack frame
//
//SYNOPSIS
//	LINK	An,#<data>
//
//	Size = (Word)
//	Size = (Word, Long)		(68020+)
//
//FUNCTION
//	This instruction saves the specified address register onto
//	the stack, then places the new stack pointer in that register. It
//	then adds the specificed immediate data to the stack pointer. To
//	allocate space on the stack for a local data area, a negative value
//	should be used for the second operand.
//
//	The use of a local stack frame is critically important to the
//	programmer who wishes to write re-entrant or recursive functions.
//	The creation of a local stack frame on the MC680x0 family is done
//	through the use of the LINK and UNLK instructions. The LINK
//	instruction saves the frame pointer onto the stack, and places a
//	pointer to the new stack frame in it. The UNLK instruction
//	restores the old stack frame. For example:
//
//		link	a5,#-8		; a5 is chosen to be the frame
//					; pointer. 8 bytes of local stack
//					; frame are allocated.
//		...
//		unlk	a5		; a5, the old frame pointer, and the
//					; old SP are restored.
//
//	Since the LINK and UNLK instructions maintain both the frame pointer
//	and the stack pointer, the following code segment is perfectly
//	legal:
//
//		link	a5,#-4
//
//		movem.l	d0-a4,-(sp)
//		pea	(500).w
//		move.l	d2,-(sp)
//		bsr.b	Routine
//
//		unlk	a5
//		rts
//
//FORMAT
//	For Word size:
//	~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|-----------|
//	| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | 1 | 0 | REGISTER  |
//	|---------------------------------------------------------------|
//	|                        16 BITS OFFSET (d)                     |
//	-----------------------------------------------------------------
//
//	"REGISTER" indicates the number of address register.
//	OFFSET is a signed value, generally negative, which enables to move
//	the stack pointer.
//
//RESULT
//	None.
	
	@Override
	public void generate() {
		int base = 0x4E50;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				LINKWord(opcode);
			}
			
		};
		
		for (int r = 0; r < 8; r++) {
			int opcode = base | r;
			cpu.addInstruction(opcode, ins);
		}
		
	}
	
	private void LINKWord(int opcode) {
		int register = opcode & 0x7;
		
		long offset  = cpu.bus.read(cpu.PC + 2) << 8;
			 offset |= cpu.bus.read(cpu.PC + 3);
		
		cpu.PC += 2;
			 
		long data = cpu.getA(register);

		cpu.SSP--;
		cpu.bus.write(cpu.SSP, data & 0xFF, Size.BYTE);
		cpu.SSP--;
		cpu.bus.write(cpu.SSP, (data >> 8) & 0xFF, Size.BYTE);
		cpu.SSP--;
		cpu.bus.write(cpu.SSP, (data >> 16) & 0xFF, Size.BYTE);
		cpu.SSP--;
		cpu.bus.write(cpu.SSP, (data >> 24), Size.BYTE);
		
		long oldSSP = cpu.SSP & 0xFFFF_FFFFL;
		
		cpu.setALong(register, oldSSP);
		
		long newSSP;
		if ((offset & 0x8000) > 0) {
			offset = -offset;
			offset &= 0xFFFF;
			newSSP = (oldSSP - offset) & 0xFFFF_FFFFL;
		} else {
			newSSP = (oldSSP + offset) & 0xFFFF_FFFFL;
		}
		
		cpu.setALong(7, newSSP);
	}

}
