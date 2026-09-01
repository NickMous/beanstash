import {Menu} from "lucide-react";
import {Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger} from "@/components/ui/sheet";
import {useTranslations} from "next-intl";
import {Link} from "@/i18n/navigation";
import {Accordion} from "@/components/ui/accordion";

interface IMenuItem {
    title: string;
    href?: string;
    subItems?: IMenuItem
}

export function Navbar() {
    const t = useTranslations("navigation");

    const menuItems: IMenuItem[] = [
        {
            title: t('home'),
            href: "/",
        },
    ]

    return (
        <nav className="flex justify-between items-center gap-4 p-4">
            <span className="font-bold">
                Beanstash
                <span className="text-xs font-normal ml-2">(think about an icon)</span>
            </span>
            <Sheet>
                <SheetTrigger>
                    <Menu/>
                </SheetTrigger>
                <SheetContent>
                    <SheetHeader>
                        <SheetTitle className="mt-1">
                            {t('whatcha_looking_for')}
                        </SheetTitle>
                    </SheetHeader>
                    <div className="flex flex-col gap-6 p-4 pt-0">
                        <Accordion>
                            {menuItems.map((menuItem) => (
                                <MobileMenuItem key={menuItem.title} menuItem={menuItem}/>
                            ))}
                        </Accordion>
                    </div>
                </SheetContent>
            </Sheet>
        </nav>
    );
}

function MobileMenuItem({menuItem}: { menuItem: IMenuItem }) {
    if (menuItem.subItems) {
        return (<p>Not implemented yet, but something with accordeon</p>)
    }

    if (!menuItem.href) {
        throw new Error('A menu item without subitems must have an url')
    }

    return (
        <Link href={menuItem.href}>
            {menuItem.title}
        </Link>
    )
}
