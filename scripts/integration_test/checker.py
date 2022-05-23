from pathlib import Path
import sys
from jinja2 import Environment, PackageLoader, select_autoescape


def intToBin32(i):
    return int((bin(((1 << 32) - 1) & i)[2:]).zfill(32), 2)

def render_program_rom(expected):
    env = Environment(
        loader=PackageLoader("checker", "templates"),
        autoescape=select_autoescape()
    )
    template = env.get_template("TopTest.scala.template")
    content = template.render(expected=expected)
    print(content)


def main():
    expected_file = Path(sys.argv[1])
    expected = []
    with expected_file.open('r') as f:
        for line in (line.strip() for line in f if line.strip() != ""):
            expected.append(intToBin32(int(line)))
    render_program_rom(expected)


if __name__ == "__main__":
    main()
