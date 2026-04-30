import math

def main():
    result = 0.0
    history = []
    undo_stack = []
    redo_stack = []

    print("Калькулятор (процедурный) — u=undo, r=redo, h=history, c=clear, e=exit")
    print("Обычные: +5 -3 *2 /4 ^3 sqrt %10\n")

    while True:
        inp = input(f"Текущее: {result:.4f}\n> ").strip()
        if not inp: continue
        low = inp.lower()

        if low in ('e', 'exit'): break

        if low in ('u', 'undo'):
            if not undo_stack:
                print("Нечего отменять.")
            else:
                redo_stack.append(result)
                result = undo_stack.pop()
            continue

        if low in ('r', 'redo'):
            if not redo_stack:
                print("Нечего повторить.")
            else:
                undo_stack.append(result)
                result = redo_stack.pop()
            continue

        if low in ('h', 'history'):
            if not history:
                print("История пуста.")
            else:
                print("История операций:")
                for i, entry in enumerate(history, 1):
                    print(f"  {i}. {entry}")
            continue

        if low in ('c', 'clear'):
            undo_stack.append(result)   # возможность отмены очистки
            result = 0.0
            history.clear()
            redo_stack.clear()
            history.append("CLEAR")
            continue

        # sqrt без параметра
        if low == 'sqrt':
            undo_stack.append(result)
            redo_stack.clear()
            try:
                if result < 0: raise ValueError("Корень из отрицательного числа")
                result = math.sqrt(result)
            except ValueError as e:
                print(f"Ошибка: {e}")
                undo_stack.pop()
                continue
            history.append("SQRT")
            continue

        # Операции с параметром
        if inp[0] in '+-*/^%':
            op = inp[0]
            val_str = inp[1:].strip()
            try:
                val = float(val_str)
            except ValueError:
                print("Неверное число."); continue
            undo_stack.append(result)
            redo_stack.clear()
            try:
                if op == '+': result += val
                elif op == '-': result -= val
                elif op == '*': result *= val
                elif op == '/':
                    if val == 0: raise ValueError("Деление на ноль")
                    result /= val
                elif op == '^': result **= val
                elif op == '%': result *= (val / 100.0)
            except ValueError as e:
                print(f"Ошибка: {e}")
                undo_stack.pop()
                continue
            history.append(f"{op}{val}")
            continue

        print("Неизвестная команда.")

if __name__ == "__main__":
    main()