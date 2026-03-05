const fs = require('fs');
const path = require('path');

const templatesDir = path.join(process.cwd(), 'src/main/resources/templates');
const subdirs = ['admin', 'doctor', 'item-manager', 'nurse', 'reservation', 'staff'];

subdirs.forEach(subdir => {
    const fullPath = path.join(templatesDir, subdir);
    if (!fs.existsSync(fullPath)) return;

    const files = fs.readdirSync(fullPath);
    files.forEach(file => {
        if (path.extname(file) === '.html') {
            const filePath = path.join(fullPath, file);
            let content = fs.readFileSync(filePath, 'utf8');

            // 1. Handle ../../index.html or ../index.html -> /
            content = content.replace(/(href|location\.href)\s*=\s*['"](\.\.\/)*index\.html['"]/g, '$1="/"');

            // 2. Handle href="name.html" -> href="/subdir/name" (relative to same subdir)
            // This is tricky if it has a different subdir in path.
            // But based on observation, they are mostly in the same folder or relative.
            
            // Match href="something.html" where something does not start with http or /
            content = content.replace(/(href|location\.href)\s*=\s*['"](?!http|\/)([^'"]+)\.html(['"])/g, (match, p1, p2, p3) => {
                // p2 is the path without .html
                if (p2 === 'index') return `${p1}="/"${p3}`;
                
                // If it contains ../, it's relative. 
                // e.g. ../admin/dashboard.html -> /admin/dashboard
                if (p2.startsWith('../')) {
                    const resolved = p2.replace(/^\.\.\//, '/');
                    return `${p1}="${resolved}"${p3}`;
                }
                
                // Otherwise it's in the same subdir
                return `${p1}="/${subdir}/${p2}"${p3}`;
            });

            const newFilePath = filePath.replace('.html', '.mustache');
            fs.writeFileSync(newFilePath, content);
            fs.unlinkSync(filePath);
            console.log(`Converted: ${file} -> ${path.basename(newFilePath)}`);
        }
    });
});

// Rename common to partials if it exists
const commonDir = path.join(templatesDir, 'common');
const partialsDir = path.join(templatesDir, 'partials');
if (fs.existsSync(commonDir)) {
    if (fs.existsSync(partialsDir)) {
        // If partials already exists, maybe move files?
        const files = fs.readdirSync(commonDir);
        files.forEach(file => {
            fs.renameSync(path.join(commonDir, file), path.join(partialsDir, file));
        });
        fs.rmdirSync(commonDir);
    } else {
        fs.renameSync(commonDir, partialsDir);
    }
    console.log('Renamed common to partials');
}
