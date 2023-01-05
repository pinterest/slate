import { TourProvider, ProviderProps } from '@reactour/tour';
import useLocalStorage from '../hooks/useLocalStorage';

const TutorialProvider: React.FC<ProviderProps> = ({ children, ...props }) => {
    const [firstTimeTour, setFirstTimeTour] = useLocalStorage<boolean>('tour-first-time', true);

    const styles = {
        popover: (base: any) => ({
            ...base,
            '--reactour-accent': '#ef5a3d',
            borderRadius: 8,
        }),
    };

    const steps = [
        {
            selector: '#builder',
            content: 'Builder helps you design and connect new and existing Resources',
        },
        {
            selector: '#executions',
            content: 'View all Resource Graph Executions submitted by you and your team and their status',
        },
        {
            selector: '#tasks',
            content: 'View all tasks running and pending tasks including approvals',
        },
        {
            selector: '#resourceCanvas',
            content: 'Canvas is where Resources can be added / removed / connected to create Resource Graph.',
        },
        {
            selector: '#resourceBtn',
            content:
                'The Resource Bar is a market place of all available Resources supported by Slate. To create new Resources drag and drop the Resource to the canvas.',
        },
        {
            selector: '#recipeBtn',
            content:
                'The Recipe Bar is a market place of popular templates. To use the Recipe, please click the recipe to view the details.',
        },
        {
            selector: '#optionBar',
            content: 'Controls to Plan and Execute Resource Graph',
        },
    ];

    return (
        <TourProvider
            {...props}
            styles={styles}
            steps={steps}
            defaultOpen={firstTimeTour}
            beforeClose={() => {
                setFirstTimeTour(false);
            }}
        >
            {children}
        </TourProvider>
    );
};

export default TutorialProvider;
