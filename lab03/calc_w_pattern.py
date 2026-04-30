import math
from abc import ABC, abstractmethod

class Calculator:
    """Хранит только текущий результат (Receiver)."""
    def __init__(self):
        self.result = 0.0

    def add(self, value: float):       self.result += value
    def subtract(self, value: float):   self.result -= value
    def multiply(self, value: float):   self.result *= value
    def divide(self, value: float):
        if value == 0: raise ValueError("Деление на ноль")
        self.result /= value
    def power(self, exponent: float):   self.result **= exponent
    def sqrt(self):
        if self.result < 0: raise ValueError("Корень из отрицательного числа")
        self.result = math.sqrt(self.result)
    def percent(self, p: float):        self.result *= (p / 100.0)

class Command(ABC):
    @abstractmethod
    def execute(self): ...
    @abstractmethod
    def undo(self): ...

class AddCommand(Command):
    def __init__(self, calc: Calculator, value: float):
        self.calc = calc; self.value = value; self._prev = None
    def execute(self):
        self._prev = self.calc.result; self.calc.add(self.value)
    def undo(self): self.calc.result = self._prev

class SubtractCommand(Command):
    def __init__(self, calc: Calculator, value: float):
        self.calc = calc; self.value = value; self._prev = None
    def execute(self):
        self._prev = self.calc.result; self.calc.subtract(self.value)
    def undo(self): self.calc.result = self._prev

class MultiplyCommand(Command):
    def __init__(self, calc: Calculator, value: float):
        self.calc = calc; self.value = value; self._prev = None
    def execute(self):
        self._prev = self.calc.result; self.calc.multiply(self.value)
    def undo(self): self.calc.result = self._prev

class DivideCommand(Command):
    def __init__(self, calc: Calculator, value: float):
        self.calc = calc; self.value = value; self._prev = None
    def execute(self):
        self._prev = self.calc.result; self.calc.divide(self.value)
    def undo(self): self.calc.result = self._prev

class PowerCommand(Command):
    def __init__(self, calc: Calculator, exponent: float):
        self.calc = calc; self.exponent = exponent; self._prev = None
    def execute(self):
        self._prev = self.calc.result; self.calc.power(self.exponent)
    def undo(self): self.calc.result = self._prev

class SqrtCommand(Command):
    def __init__(self, calc: Calculator):
        self.calc = calc; self._prev = None
    def execute(self):
        self._prev = self.calc.result; self.calc.sqrt()
    def undo(self): self.calc.result = self._prev

class PercentCommand(Command):
    def __init__(self, calc: Calculator, percent: float):
        self.calc = calc; self.percent = percent; self._prev = None
    def execute(self):
        self._prev = self.calc.result; self.calc.percent(self.percent)
    def undo(self): self.calc.result = self._prev

class ClearCommand(Command):
    def __init__(self, calc: Calculator, invoker: 'CalculatorInvoker'):
        self.calc = calc; self.invoker = invoker
        self._prev_result = None; self._prev_history = []; self._prev_redo = []
    def execute(self):
        self._prev_result = self.calc.result
        self._prev_history = list(self.invoker._history)
        self._prev_redo = list(self.invoker._redo_stack)
        self.calc.result = 0.0
        self.invoker._history.clear()
        self.invoker._redo_stack.clear()
    def undo(self):
        self.calc.result = self._prev_result
        self.invoker._history = list(self._prev_history)
        self.invoker._redo_stack = list(self._prev_redo)

class MacroCommand(Command):
    def __init__(self, commands: list[Command]):
        self.commands = commands
    def execute(self):
        for cmd in self.commands: cmd.execute()
    def undo(self):
        for cmd in reversed(self.commands): cmd.undo()

class CalculatorInvoker:
    def __init__(self):
        self._history: list[Command] = []
        self._redo_stack: list[Command] = []

    def execute_command(self, command: Command) -> float | None:
        command.execute()
        self._history.append(command)
        self._redo_stack.clear()
        if hasattr(command, 'calc'):
            return command.calc.result
        return None

    def undo(self) -> float | None:
        if not self._history: return None
        cmd = self._history.pop()
        cmd.undo()
        self._redo_stack.append(cmd)
        return getattr(cmd, 'calc', None) and cmd.calc.result

    def redo(self) -> float | None:
        if not self._redo_stack: return None
        cmd = self._redo_stack.pop()
        cmd.execute()
        self._history.append(cmd)
        return getattr(cmd, 'calc', None) and cmd.calc.result

    def show_history(self):
        if not self._history:
            print("История пуста."); return
        print("История операций:")
        for i, cmd in enumerate(self._history, 1):
            match cmd:
                case AddCommand(value=v):      op = f"+ {v}"
                case SubtractCommand(value=v): op = f"- {v}"
                case MultiplyCommand(value=v): op = f"* {v}"
                case DivideCommand(value=v):   op = f"/ {v}"
                case PowerCommand(exponent=e): op = f"^ {e}"
                case SqrtCommand():            op = "sqrt"
                case PercentCommand(percent=p):op = f"% {p}"
                case ClearCommand():           op = "CLEAR"
                case MacroCommand(commands=cmds): op = f"MACRO ({len(cmds)} ops)"
                case _: op = "?"
            print(f"  {i}. {op}")

def parse_macro(calc: Calculator, macro_str: str) -> MacroCommand | None:
    tokens = macro_str.split()
    commands = []
    for tok in tokens:
        op = tok[0]; val_str = tok[1:]
        try:
            val = float(val_str)
        except ValueError:
            print(f"Неверное число в макросе: {tok}"); return None
        if op == '+':      cmd = AddCommand(calc, val)
        elif op == '-':    cmd = SubtractCommand(calc, val)
        elif op == '*':    cmd = MultiplyCommand(calc, val)
        elif op == '/':    cmd = DivideCommand(calc, val)
        elif op == '^':    cmd = PowerCommand(calc, val)
        elif op == '%':    cmd = PercentCommand(calc, val)
        else:
            print(f"Неизвестная операция в макросе: {op}"); return None
        commands.append(cmd)
    return MacroCommand(commands) if commands else None

def main():
    calc = Calculator()
    invoker = CalculatorInvoker()
    print("Калькулятор (паттерн Команда) — u=undo, r=redo, h=history, c=clear, e=exit")
    print("Обычные: +5 -3 *2 /4 ^3 sqrt %10")
    print("Макрос: macro +5 *2 /3\n")

    while True:
        inp = input(f"Текущее: {calc.result:.4f}\n> ").strip()
        if not inp: continue
        low = inp.lower()

        if low in ('e', 'exit'): break
        if low in ('u', 'undo'):
            res = invoker.undo()
            if res is not None: calc.result = res
            continue
        if low in ('r', 'redo'):
            res = invoker.redo()
            if res is not None: calc.result = res
            continue
        if low in ('h', 'history'):
            invoker.show_history()
            continue
        if low in ('c', 'clear'):
            invoker.execute_command(ClearCommand(calc, invoker))
            calc.result = 0.0
            continue

        # Макрос
        if low.startswith('macro '):
            macro_cmd = parse_macro(calc, inp[6:])
            if macro_cmd:
                invoker.execute_command(macro_cmd)
                print(f"Макрос выполнен, результат: {calc.result:.4f}")
            continue

        # sqrt (без параметра)
        if low == 'sqrt':
            try:
                invoker.execute_command(SqrtCommand(calc))
            except ValueError as e:
                print(f"Ошибка: {e}")
            continue

        # Операции с параметром
        if inp[0] in '+-*/^%':
            op = inp[0]
            val_str = inp[1:].strip()
            try:
                value = float(val_str)
            except ValueError:
                print("Неверное число."); continue
            try:
                if op == '+': cmd = AddCommand(calc, value)
                elif op == '-': cmd = SubtractCommand(calc, value)
                elif op == '*': cmd = MultiplyCommand(calc, value)
                elif op == '/': cmd = DivideCommand(calc, value)
                elif op == '^': cmd = PowerCommand(calc, value)
                elif op == '%': cmd = PercentCommand(calc, value)
                else: continue
                res = invoker.execute_command(cmd)
                if res is not None: calc.result = res
            except ValueError as e:
                print(f"Ошибка: {e}")
            continue

        print("Неизвестная команда.")

if __name__ == "__main__":
    main()