# sistema-operacional

## Implementação
Em nossa maquina virtual o grupo decidiu que era mais vantajoso continuar com a implementação provida em java previamente pelo professor, logo
seu funcionamento continua muito proximo a tal e com isso, para testar as novas funcionalidades disponibilizamos alguns programas que serão demonstrados
em **Usabilidade**

## Usabilidade
Para conseguir acessar e testar os programas disponiveis, deve-se entrar na file **Sistema.java** e encontrar a função "main" como demonstrado no exemplo
abaixo.

```
public static void main(String args[]) {
		// PROGRAMA NORMAL FIBONACCI

		// Sistema s = new Sistema();
		// s.test1();

		// PROGRAMA INTERRUPÇÃO ENDEREÇO INVÁLIDO

		// Sistema s = new Sistema();
		// s.errorMemory();

		// PROGRAMA TESTE CHAMADA DE SISTEMA INPUT

		// Sistema s = new Sistema();
		// s.programTestTrapInput();

		// PROGRAMA TESTE CHAMADA DE SISTEMA OUTPUT

		// Sistema s = new Sistema();
		// s.programTestTrapOutput();

		// PROGRAMA INTERRUPÇÃO INSTRUÇÃO INVÁLIDA

		// Sistema s = new Sistema();
		// s.programTestInvalidInstruction();

		// PROGRAMA TESTE OVERFLOW

		Sistema s = new Sistema();
		s.programTestOverflow();
	}
  ```
  Nesse metódo pode-se perceber que todos os programas menos um estão comentados, para conseguir testar os demais comente o atual e descomente o
  que dejesa testar, no código como demonstrado acima há comentarios para cada programa, expecificando sua funcionalidade.
