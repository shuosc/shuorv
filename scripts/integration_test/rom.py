import sys
from pathlib import Path

from jinja2 import Environment, PackageLoader, select_autoescape


def render_program_rom(address_table, code_table):
    env = Environment(
        loader=PackageLoader("rom", "templates"),
        autoescape=select_autoescape()
    )
    template = env.get_template("ProgramROM.scala.template")
    content = template.render(
        address_table=address_table, code_table=code_table)
    print(content)


def main():
    lot_file = Path(sys.argv[1])
    address_table = {}
    with lot_file.open('r') as f:
        for line in (l.strip() for l in f.readlines() if l.strip() != ""):
            [section_name, section_address] = line.split(":")
            section_address = int(section_address, 16)
            address_table[section_name] = section_address
    code_table = {}
    for section in address_table.keys():
        path = lot_file.parent.joinpath('{}.thex'.format(section))
        with path.open('r') as f:
            code_table[section] = [l.strip()
                                   for l in f.readlines() if l.strip() != ""]
    render_program_rom(address_table, code_table)


if __name__ == "__main__":
    main()
