import sys
import requests
import urllib.request
import time
from bs4 import BeautifulSoup
import re

'''
This script takes either 0 or 1 arguments, the path to the file to output to.
If the argument is not provided this defaults to "available-rules.xml" in the
directory from which the script was run.
'''

extensions = ['annotation', 'blocks', 'design', 'coding', 'header', 'imports',
              'javadoc', 'metrics', 'misc', 'modifier', 'naming', 'regexp', 'sizes', 'whitespace']

tmp = {}


def get_url(extension):
    return 'http://checkstyle.sourceforge.net/config_' + extension + '.html'

def get_page(extension):
  url = get_url(extension)
  response = requests.get(url)
  return BeautifulSoup(response.text, 'html.parser')

def get_category_list(extension, out):
  soup = get_page(extension)

  return [
    '<category name="' + soup.title.text[13:] + '">', # Category Name is in page title after "checkstyle - "
    get_rules_list(soup, 2 if extension == 'naming' else 1),
    '</category>'
  ]

def write_lines(out, lines, indent_level=0, spaces_per_indent=2):
  for line in lines:
    if (type(line) == type('str')):
      out.write((' ' * indent_level * spaces_per_indent) + line + '\n')
    else: 
      write_lines(out, line, indent_level + 1, spaces_per_indent)

def get_header_lines():
  return [
    '<?xml version="1.0"?>',
    '<!DOCTYPE root>'
  ]

def get_checker_lines():
  return [
    '<module name="Checker" description="All configurations have root module Checker.">',
    [
      '<property name="basedir" description="base directory name; stripped off in messages about files" type="String" default="null"/>',
      '<property name="cacheFile" description="caches information about files that have checked OK; used to avoid repeated checks of the same files" type="File" default="null"/>',
      '<property name="localeCountry" description="locale country for messages" type="String" default="default locale country for the Java Virtual Machine"/>',
      '<property name="localeLanguage" description="locale language for messages" type="String" default="default locale language for the Java Virtual Machine"/>',
      '<property name="charset" description="name of the file charset" type="String" default="System property &quot;file.encoding&quot;"/>',
      '<property name="fileExtensions" description="file extensions that are accepted" type="String Set" default="all files"/>',
      '<property name="severity" description="the default severity level of all violations" type="Severity" default="error"/>',
      '<property name="haltOnException" description="whether to stop execution of Checkstyle if a single file produces any kind of exception during verification" type="Boolean" default="true"/>'
    ],
    '</module>'
  ]

def get_treewalker_lines():
  return [
    '<module name="TreeWalker" parent="Checker" description="FileSetCheck TreeWalker checks individual Java source files and defines properties that are applicable to checking such files.">',
    [
      '<property name="tabWidth" description="number of expanded spaces for a tab character (&quot;\\t&quot;); used in messages and Checks that require a tab width, such as LineLength" type="Integer" default="8"/>',
      '<property name="fileExtensions" description="file type extension to identify Java files. Setting this property is typically only required if your Java source code is preprocessed before compilation and the original files do not have the extension .java" type="String Set" default=".java"/>',
    ],
    '</module>'
  ]

def get_rules_list(soup, start_idx):
  rules = soup.body.find(id='contentBox').find_all(class_='section', recursive=False)[start_idx:]

  return [get_rule_list(rule) for rule in rules]

def get_rule_list(rule):
  rule_secs = rule.find_all(class_='section', recursive=False)
  rule_sec_idxs = {rule_section.h3.text: idx for idx, rule_section in enumerate(rule_secs)}

  attr_table = rule_secs[rule_sec_idxs['Properties']].table \
    if 'Properties' in rule_sec_idxs.keys() else None

  parent = rule_secs[rule_sec_idxs['Parent Module']].p.a.text
  tmp[parent] = tmp.setdefault(parent, 0) + 1

  return [
    get_rule_open_line(
      name=rule.h2.text,
      parent=parent,
      description=get_rule_description(rule_secs[rule_sec_idxs['Description']])
    ),
    [
      *get_attributes_list(attr_table)
    ],
    '</module>'
  ]

def get_rule_open_line(name='', parent='', description=''):
  return '<module name="' + name + '" parent="' + parent + '" description="' + description + '">'

def get_rule_description(description_section):
  rule_desc_p_lst = description_section.find_all('p', recursive=False)
  rule_desc_p_idx = 1 if rule_desc_p_lst[0].text[:5] == 'Since' else 0 # Find first sentence of description
  rule_desc = escape_raw_string(rule_desc_p_lst[rule_desc_p_idx].text)
  rule_desc = re.sub('(?<!e\.g)(?<!i\.e)\. .*', '.', rule_desc) # Trim everything after the first sentence

  return rule_desc

def get_attributes_list(attr_table):
  if attr_table is None:
    return []

  attr_rows = attr_table.find_all('tr')
  col_idxs = {} # Stores the index of each column name in the table

  for idx, header in enumerate(attr_rows[0].find_all('th')): # Find column indexes from header row
    col_idxs[header.text] = idx

  return [
    get_attribute_line(
      attr_row,
      name_idx=col_idxs['name'],
      desc_idx=col_idxs['description'],
      type_idx=col_idxs['type'],
      default_idx=col_idxs['default value']
    )
    for attr_row in attr_rows[1:]
  ]

def get_attribute_line(attr_row, name_idx=0, desc_idx=1, type_idx=2, default_idx=3):
  attr_cells = attr_row.find_all('td')
  attr_name = attr_cells[name_idx].text
  attr_desc = escape_raw_string(attr_cells[desc_idx].text)
  attr_type = escape_raw_string(attr_cells[type_idx].text)
  attr_default = escape_raw_string(attr_cells[default_idx].text, quot_escape = '')

  return (
    '<property name="' + attr_name + 
    '" description="' + attr_desc + 
    '" type="' + attr_type + 
    '" default="' + attr_default + 
    '"/>'
  )

def escape_raw_string(
  raw,
  amp_escape='&amp;',
  quot_escape='&quot;',
  lt_escape='&lt;',
  gt_escape='&gt;'
):
  escaped = (
    raw
    .replace('\n', '')
    .replace('&', amp_escape)
    .replace('"', quot_escape)
    .replace('<', lt_escape)
    .replace('>', gt_escape)
    .strip()
  )
  return re.sub(' +', ' ', escaped)

if len(sys.argv) == 2:
  file_name = sys.argv[1]
else:
  file_name = 'available-rules.xml'

out = open(file_name, 'w')

xml = get_header_lines() + [
  '<root>',
  get_checker_lines(),
  get_treewalker_lines(),
  *[get_category_list(extension, out) for extension in extensions],
  '</root>'
]
write_lines(out, xml)
out.close()
print(tmp)
