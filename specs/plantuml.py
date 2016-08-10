#!/usr/bin/env python

"""
Pandoc filter to process code blocks with class "plantuml" into
plant-generated images.

Needs `plantuml.jar` from http://plantuml.com/.
"""

import os
import sys
from subprocess import call
import hashlib
import os

from pandocfilters import toJSONFilter, Para, Image



def get_filename4code(module, content, ext=None):
    """Generate filename based on content
    The function ensures that the (temporary) directory exists, so that the
    file can be written.
    Example:
        filename = get_filename4code("myfilter", code)
    """
    imagedir = module + "-images"
    fn = hashlib.sha1(content.encode(sys.getfilesystemencoding())).hexdigest()
    try:
        os.mkdir(imagedir)
        sys.stderr.write('Created directory ' + imagedir + '\n')
    except OSError:
        pass
    if ext:
        fn += "." + ext
    return os.path.join(imagedir, fn)


def get_caption(kv):
    """get caption from the keyvalues (options)
    Example:
      if key == 'CodeBlock':
        [[ident, classes, keyvals], code] = value
        caption, typef, keyvals = get_caption(keyvals)
        ...
        return Para([Image([ident, [], keyvals], caption, [filename, typef])])
    """
    res = []
    caption = []
    typef = ""
    for k, v in kv:
        if k == u"caption":
            caption = [Str(v)]
            typef = "fig:"
        else:
            res.append([k, v])

    return caption, typef, res


def get_extension(format, default, **alternates):
    """get the extension for the result, needs a default and some specialisations
    Example:
      filetype = get_extension(format, "png", html="svg", latex="eps")
    """
    try:
        return alternates[format]
    except KeyError:
        return default


def plantuml(key, value, format, _):
    if key == 'CodeBlock':
        [[ident, classes, keyvals], code] = value

        if "plantuml" in classes:
            caption, typef, keyvals = get_caption(keyvals)

            filename = get_filename4code("plantuml", code)
            filetype = get_extension(format, "png", html="svg", latex="eps")

            src = filename + '.uml'
            dest = filename + '.' + filetype

            if not os.path.isfile(dest):
                txt = code.encode(sys.getfilesystemencoding())
                if not txt.startswith("@start"):
                    txt = "@startuml\n" + txt + "\n@enduml\n"
                with open(src, "w") as f:
                    f.write(txt)

                call(["java", "-jar", "plantuml.jar", "-t"+filetype, src])
                sys.stderr.write('Created image ' + dest + '\n')

            return Para([Image([ident, [], keyvals], caption, [dest, typef])])

if __name__ == "__main__":
    toJSONFilter(plantuml)
