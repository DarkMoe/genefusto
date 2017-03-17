package gen;

public abstract class GenInstruction {

	public GenInstruction() {
	}
	
	Size mapSize(int siz) {
		if (siz == 0b01) {
			return Size.byt;
		} else if (siz == 0b11) {
			return Size.word;
		} else if (siz == 0b10) {
			return Size.longW;
		}
		return null;
	}
	
//	SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
	Size mapAlternateSize(int size) {
		if (size == 0b00) {
			return Size.byt;
		} else if (size == 0b01) {
			return Size.word;
		} else if (size == 0b10) {
			return Size.longW;
		}
		return null;
	}
	
//	OP-MODE
//	Byte	Word	Long
//	~~~~	~~~~	~~~~
//	000		001		010	(Dn) - (<ea>) -> Dn
//	100		101		110	(<ea>) - (Dn) -> <ea>
	Size mapFromOpMode(int size) {
		if (size == 0b000 || size == 0b100) {
			return Size.byt;
		} else if (size == 0b001 || size == 0b101) {
			return Size.word;
		} else if (size == 0b010 || size == 0b110) {
			return Size.longW;
		}
		return null;
	}
	

	public GenInstruction generate(GenInstruction[] set) {
		return null;
		
	}

	public abstract void run(int opcode);
}
